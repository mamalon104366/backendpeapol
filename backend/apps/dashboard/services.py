"""Cálculo del resumen agregado de una empresa (compartido por dashboard y chat)."""
from django.db.models import Avg, Count
from django.db.models.functions import TruncMonth

from apps.forms.models import IndicatorScore, Module
from apps.forms.models import Response as SurveyResponse
from apps.forms.scoring import band

MES_ES = ["", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"]


def _monthly(qs):
    out = {}
    for row in (
        qs.annotate(m=TruncMonth("response__submitted_at")).values("m").annotate(a=Avg("score")).order_by("m")
    ):
        if row["m"] is not None:
            out[row["m"]] = float(row["a"])
    return out


def compute_summary(company):
    modules = {m.id: m for m in Module.objects.all()}
    scores = IndicatorScore.objects.filter(response__company=company)
    responses = SurveyResponse.objects.filter(company=company)
    employees = company.employees.count() if company else 0

    if not scores.exists():
        return _empty(company, employees, responses.count())

    indicators = []
    for row in scores.values("module").annotate(avg=Avg("score")).order_by("module"):
        mid, v = row["module"], round(float(row["avg"]), 1)
        indicators.append({"module": mid, "label": modules[mid].label,
                           "invert": modules[mid].invert, "score": v, "band": band(v)})

    well_m = _monthly(scores)
    burn_m = _monthly(scores.filter(module_id="burnout"))
    ren_m = _monthly(scores.filter(module_id="renuncia"))
    eng_m = _monthly(scores.filter(module_id__in=["motivacion", "liderazgo"]))
    months = sorted(well_m.keys())

    def cur_prev(d):
        cur = d.get(months[-1]) if months else None
        prev = d.get(months[-2]) if len(months) > 1 else None
        return cur, prev

    def kpi(key, label, series, invert, hint):
        cur, prev = cur_prev(series)
        cur = cur if cur is not None else 0
        value = round(100 - cur) if invert else round(cur)
        delta = 0.0
        if prev is not None:
            cv = (100 - cur) if invert else cur
            pv = (100 - prev) if invert else prev
            delta = round(cv - pv, 1)
        return {"key": key, "label": label, "value": value, "delta": delta, "invert": invert, "hint": hint}

    overall = round(float(scores.aggregate(a=Avg("score"))["a"]), 1)
    at_risk = _at_risk(company)
    kpis = [
        kpi("wellbeing", "Índice de bienestar", well_m, False, "salud organizacional"),
        kpi("retention", "Riesgo de renuncia", ren_m, True, f"{len(at_risk)} en alerta"),
        kpi("burnout", "Burnout promedio", burn_m, True, "global de la empresa"),
        kpi("engagement", "Compromiso", eng_m, False, "motivación + liderazgo"),
    ]

    trend = []
    for m in months:
        riesgo = ren_m.get(m)
        trend.append({"mes": MES_ES[m.month], "bienestar": round(well_m[m], 1),
                      "riesgo": round(100 - riesgo, 1) if riesgo is not None else None})

    head = {r["department__name"]: r["n"]
            for r in company.employees.values("department__name").annotate(n=Count("id"))}
    by_area = []
    for arow in scores.values("response__area").annotate(n=Count("id")).order_by("response__area"):
        area = arow["response__area"] or "Sin área"
        asc = scores.filter(response__area=arow["response__area"])
        bh = asc.filter(module_id="burnout").aggregate(a=Avg("score"))["a"]
        wh = asc.aggregate(a=Avg("score"))["a"]
        by_area.append({"area": area, "burnout": round(100 - float(bh), 1) if bh is not None else 0,
                        "bienestar": round(float(wh), 1) if wh is not None else 0,
                        "headcount": head.get(area, 0)})
    by_area.sort(key=lambda x: -x["burnout"])

    alerts = []
    for a in by_area:
        if a["burnout"] >= 70:
            alerts.append({"level": "risk", "area": a["area"], "title": "Burnout crítico",
                           "detail": "Carga elevada y baja satisfacción sostenida.", "metric": f"{round(a['burnout'])}%"})
        elif a["burnout"] >= 58:
            alerts.append({"level": "warn", "area": a["area"], "title": "Clima en descenso",
                           "detail": "Indicadores por debajo del objetivo del área.", "metric": f"{round(a['burnout'])}%"})
    alerts = alerts[:4]

    dist = {"good": 0, "warn": 0, "risk": 0}
    for r in scores.values("response").annotate(a=Avg("score")):
        dist[band(float(r["a"]))] += 1

    distinct_emp = responses.filter(employee__isnull=False).values("employee").distinct().count()
    return {
        "company": company.name if company else None,
        "employees": employees,
        "responses": responses.count(),
        "response_rate": round(distinct_emp / employees, 2) if employees else 0,
        "overall_wellbeing": overall,
        "kpis": kpis, "indicators": indicators, "trend": trend,
        "by_area": by_area, "alerts": alerts, "distribution": dist, "at_risk": at_risk,
    }


def _at_risk(company):
    best = {}
    rows = (IndicatorScore.objects.filter(
        response__company=company, module_id="renuncia", response__employee__isnull=False)
        .select_related("response__employee__department").order_by("response__submitted_at"))
    for s in rows:
        e = s.response.employee
        best[e.id] = {"name": e.full_name, "area": e.department.name if e.department else "—",
                      "role": e.role_title or "—", "risk": round(100 - float(s.score))}
    return sorted(best.values(), key=lambda x: -x["risk"])[:5]


def _empty(company, employees, responses):
    return {"company": company.name if company else None, "employees": employees, "responses": responses,
            "response_rate": 0, "overall_wellbeing": None, "kpis": [], "indicators": [], "trend": [],
            "by_area": [], "alerts": [], "distribution": {"good": 0, "warn": 0, "risk": 0}, "at_risk": []}
