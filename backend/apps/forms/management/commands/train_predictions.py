"""
Entrena un Random Forest para predecir riesgo de renuncia y lo aplica a los
empleados reales (a partir de sus indicadores). Persiste Prediction + Alert y
guarda métricas del modelo en ml/meta.json.

    python manage.py train_predictions

NOTA DE HONESTIDAD: no tenemos etiquetas reales de "quién renunció", así que el
modelo se entrena con un dataset SINTÉTICO (Fase 2 del plan). La relación
features→etiqueta es conocida + ruido, lo que demuestra el pipeline de ML real.
"""
import json
from pathlib import Path

import joblib
import numpy as np
from django.conf import settings
from django.core.management.base import BaseCommand
from django.db.models import Avg
from django.utils import timezone
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, roc_auc_score
from sklearn.model_selection import train_test_split

from apps.companies.models import Company
from apps.forms.models import Alert, IndicatorScore, Module, Prediction

MODULE_ORDER = [
    "burnout", "clima", "estres", "motivacion", "liderazgo",
    "satisfaccion", "renuncia", "equipo", "productividad", "capacitacion",
]
# Peso de cada módulo en la propensión a renunciar (salud BAJA → más riesgo)
WEIGHTS = {
    "renuncia": 2.2, "burnout": 1.4, "satisfaccion": 1.1, "motivacion": 0.9,
    "liderazgo": 0.8, "estres": 0.7, "clima": 0.5, "equipo": 0.3,
    "productividad": 0.3, "capacitacion": 0.4,
}
ML_DIR = Path(settings.BASE_DIR) / "ml"


def sigmoid(x):
    return 1 / (1 + np.exp(-x))


def make_synthetic(n, rng):
    """Genera n perfiles (salud 0–100 por módulo) + etiqueta renunció(1/0)."""
    X = np.zeros((n, len(MODULE_ORDER)))
    latent = rng.uniform(25, 80, n)
    for j in range(len(MODULE_ORDER)):
        X[:, j] = np.clip(rng.normal(latent, 12), 5, 95)
    w = np.array([WEIGHTS[m] for m in MODULE_ORDER])
    logit = -1.0 + ((50 - X) / 50) @ w + rng.normal(0, 0.6, n)
    p = sigmoid(logit)
    y = (rng.uniform(0, 1, n) < p).astype(int)
    return X, y


class Command(BaseCommand):
    help = "Entrena el Random Forest de renuncia y genera predicciones + alertas."

    def handle(self, *args, **opts):
        rng = np.random.default_rng(42)
        company = Company.objects.filter(name="Andes Logística").first()
        if not company:
            self.stderr.write("✖ Falta la empresa demo (seed_catalog).")
            return

        # ---- 1. Entrenar con datos sintéticos ----
        X, y = make_synthetic(600, rng)
        Xtr, Xte, ytr, yte = train_test_split(X, y, test_size=0.25, random_state=42, stratify=y)
        model = RandomForestClassifier(
            n_estimators=200, max_depth=6, random_state=42, class_weight="balanced"
        )
        model.fit(Xtr, ytr)
        proba_te = model.predict_proba(Xte)[:, 1]
        acc = accuracy_score(yte, model.predict(Xte))
        auc = roc_auc_score(yte, proba_te)

        labels = {m.id: m.label for m in Module.objects.all()}
        importances = sorted(
            ({"module": m, "label": labels.get(m, m), "importance": round(float(imp), 4)}
             for m, imp in zip(MODULE_ORDER, model.feature_importances_)),
            key=lambda d: -d["importance"],
        )
        self.stdout.write(f"  modelo: acc={acc:.3f} auc={auc:.3f} base_rate={y.mean():.2f}")

        # ---- 2. Vectores de los empleados reales ----
        scores = IndicatorScore.objects.filter(
            response__company=company, response__employee__isnull=False
        )
        comp_mean = {
            r["module"]: float(r["a"])
            for r in scores.values("module").annotate(a=Avg("score"))
        }
        emp_scores: dict[int, dict[str, float]] = {}
        for r in scores.values("response__employee", "module").annotate(a=Avg("score")):
            emp_scores.setdefault(r["response__employee"], {})[r["module"]] = float(r["a"])

        from apps.companies.models import Employee
        employees = {e.id: e for e in Employee.objects.filter(company=company).select_related("department")}

        imp_map = {d["module"]: d["importance"] for d in importances}
        Prediction.objects.filter(company=company).delete()
        preds = []
        rows = []
        for eid, mods in emp_scores.items():
            vec = np.array([[mods.get(m, comp_mean.get(m, 50.0)) for m in MODULE_ORDER]])
            prob = float(model.predict_proba(vec)[0, 1])
            # factor dominante = módulo con mayor (importancia * déficit de salud)
            factor = max(
                MODULE_ORDER,
                key=lambda m: imp_map.get(m, 0) * max(0, (50 - mods.get(m, comp_mean.get(m, 50))) / 50),
            )
            emp = employees[eid]
            preds.append(Prediction(
                company=company, employee=emp, kind="renuncia",
                score=round(prob * 100, 2), top_factor=labels.get(factor, factor),
            ))
            rows.append((emp, prob, emp.department.name if emp.department else "—"))
        Prediction.objects.bulk_create(preds)
        rows.sort(key=lambda r: -r[1])

        # ---- 3. Alertas (áreas + fuga de talento) ----
        Alert.objects.filter(company=company).delete()
        alerts = []
        area_burnout = {}
        for ar in scores.values("response__area").distinct():
            area = ar["response__area"]
            bh = scores.filter(response__area=area, module_id="burnout").aggregate(a=Avg("score"))["a"]
            if bh is not None:
                area_burnout[area or "Sin área"] = round(100 - float(bh), 1)
        for area, b in sorted(area_burnout.items(), key=lambda x: -x[1]):
            if b >= 70:
                alerts.append(Alert(company=company, level="risk", area=area, title="Burnout crítico",
                                    detail="Carga elevada y baja satisfacción sostenida. Reducir carga laboral.",
                                    metric=f"{round(b)}%"))
            elif b >= 58:
                alerts.append(Alert(company=company, level="warn", area=area, title="Clima en descenso",
                                    detail="Indicadores por debajo del objetivo del área.", metric=f"{round(b)}%"))
        high = [r for r in rows if r[1] >= 0.6]
        if len(high) >= 3:
            top_area = high[0][2]
            alerts.append(Alert(company=company, level="risk", area=top_area, title="Fuga de talento",
                                detail=f"{len(high)} colaboradores con alta probabilidad de renuncia.",
                                metric=f"{round(high[0][1]*100)}%"))
        Alert.objects.bulk_create(alerts[:5])

        # ---- 4. Guardar modelo + metadatos ----
        ML_DIR.mkdir(exist_ok=True)
        joblib.dump(model, ML_DIR / "model.joblib")
        meta = {
            "trained_at": timezone.now().isoformat(),
            "algorithm": "RandomForestClassifier",
            "n_estimators": 200,
            "n_train": 600,
            "test_accuracy": round(float(acc), 3),
            "roc_auc": round(float(auc), 3),
            "base_rate": round(float(y.mean()), 3),
            "synthetic_labels": True,
            "importances": importances,
            "employees_scored": len(preds),
        }
        (ML_DIR / "meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False), encoding="utf-8")

        self.stdout.write(self.style.SUCCESS(
            f"✅ {len(preds)} predicciones · {len(alerts[:5])} alertas · top driver: {importances[0]['label']}"
        ))
