"""
Siembra respuestas sintéticas para que el dashboard tenga datos reales.
Crea (si falta) un formulario integral de 10 módulos y genera ~6 meses de
respuestas con sesgo por área (Soporte quemado, RR.HH./Finanzas sanos) y una
leve mejora mes a mes para la tendencia.

    python manage.py seed_responses          # idempotente (omite si ya hay)
    python manage.py seed_responses --reset   # borra y vuelve a sembrar
"""
import random
from datetime import datetime, timezone as tz

from django.core.management.base import BaseCommand
from django.db import transaction

from apps.companies.models import Company, Employee
from apps.forms.models import (
    Form,
    FormQuestion,
    FormRecipient,
    IndicatorScore,
    Module,
    Response,
    ResponseDetail,
)

# (burnout_raw, wellbeing_baseline) por área — narrativa del producto
AREA_TARGETS = {
    "Soporte": (82, 41),
    "Ventas": (67, 55),
    "Operaciones": (61, 58),
    "Tecnología": (54, 66),
    "Finanzas": (39, 74),
    "Marketing": (44, 70),
    "RR.HH.": (33, 79),
}
FORM_NAME = "Diagnóstico Organizacional 360°"
MONTHS = [(2026, m) for m in range(1, 7)]  # ene–jun 2026


def clamp(v, lo, hi):
    return max(lo, min(hi, v))


def sample_value(avg):
    return clamp(round(random.gauss(avg, 0.8)), 1, 5)


class Command(BaseCommand):
    help = "Siembra respuestas sintéticas para el dashboard."

    def add_arguments(self, parser):
        parser.add_argument("--reset", action="store_true", help="Borra respuestas previas y resiembra.")

    @transaction.atomic
    def handle(self, *args, **opts):
        random.seed(42)
        company = Company.objects.filter(name="Andes Logística").first()
        if not company:
            self.stderr.write("✖ Falta la empresa demo. Corre primero: seed_catalog")
            return

        form = self._ensure_form(company)

        existing = Response.objects.filter(form=form).count()
        employees = list(Employee.objects.filter(company=company).select_related("department"))
        if not employees:
            self.stderr.write("✖ No hay empleados. Corre seed_catalog.")
            return

        self._ensure_recipients(form, employees)
        if existing and not opts["reset"]:
            self.stdout.write(f"• Ya hay {existing} respuestas. Usa --reset para resembrar.")
            return
        if opts["reset"]:
            Response.objects.filter(form=form).delete()
            FormRecipient.objects.filter(form=form).update(
                status=FormRecipient.Status.PENDING,
                completed_at=None,
            )

        modules = {m.id: m for m in Module.objects.all()}
        questions = list(form.questions.select_related("module"))

        total = 0
        for mi, (year, month) in enumerate(MONTHS):
            months_ago = (len(MONTHS) - 1) - mi  # 5..0 (0 = mes más reciente)
            # ~18 respuestas por mes
            for _ in range(18):
                emp = random.choice(employees)
                area = emp.department.name if emp.department else "Operaciones"
                b_raw, well = AREA_TARGETS.get(area, (55, 60))
                # mejora con el tiempo (meses viejos = peor)
                b_month = clamp(b_raw + months_ago * 1.6, 5, 95)
                w_month = clamp(well - months_ago * 1.3, 5, 95)

                details = []
                module_values: dict[str, list[int]] = {}
                for q in questions:
                    mod = q.module
                    if mod.invert:
                        jitter = {"burnout": 0, "estres": -4, "renuncia": -8}.get(mod.id, 0)
                        avg = 1 + 4 * clamp(b_month + jitter, 5, 95) / 100
                    else:
                        avg = 1 + 4 * w_month / 100
                    val = sample_value(avg)
                    details.append(ResponseDetail(form_question=q, value=val))
                    module_values.setdefault(mod.id, []).append(val)

                resp = Response.objects.create(
                    form=form, company=company, employee=emp,
                    employee_name=emp.full_name, area=area,
                )
                day = random.randint(1, 27)
                dt = datetime(year, month, day, 10, 0, tzinfo=tz.utc)
                Response.objects.filter(pk=resp.pk).update(submitted_at=dt)

                for d in details:
                    d.response = resp
                ResponseDetail.objects.bulk_create(details)

                scores = []
                for mid, vals in module_values.items():
                    avg = sum(vals) / len(vals)
                    pct = ((avg - 1) / 4) * 100
                    if modules[mid].invert:
                        pct = 100 - pct
                    scores.append(IndicatorScore(response=resp, module_id=mid, score=round(pct, 2)))
                IndicatorScore.objects.bulk_create(scores)
                total += 1

        self.stdout.write(self.style.SUCCESS(
            f"✅ {total} respuestas sembradas en «{form.name}» "
            f"(total empresa: {Response.objects.filter(company=company).count()})"
        ))

    def _ensure_form(self, company):
        form = Form.objects.filter(company=company, name=FORM_NAME).first()
        if form:
            return form
        form = Form.objects.create(
            company=company, name=FORM_NAME,
            description="Encuesta integral de clima, bienestar y riesgo (todos los módulos).",
            status=Form.Status.ACTIVE, ai_generated=True, scale_max=5,
        )
        mods = list(Module.objects.all())
        form.modules.set(mods)
        ord_ = 0
        for m in mods:
            for mq in m.questions.all():
                FormQuestion.objects.create(form=form, module=m, text=mq.text, ord=ord_)
                ord_ += 1
        return form

    def _ensure_recipients(self, form, employees):
        existing_ids = set(
            FormRecipient.objects.filter(form=form).values_list("employee_id", flat=True)
        )
        to_create = [
            FormRecipient(form=form, employee=employee)
            for employee in employees
            if employee.id not in existing_ids
        ]
        if to_create:
            FormRecipient.objects.bulk_create(to_create, ignore_conflicts=True)
