"""Cálculo de indicadores — espejo de src/lib/scoring.ts del frontend."""
from collections import defaultdict


def band(score: float) -> str:
    if score >= 70:
        return "good"
    if score >= 45:
        return "warn"
    return "risk"


def compute_module_scores(response):
    """
    Lee los ResponseDetail de una respuesta y devuelve la salud 0–100 por módulo.
    Para módulos invert (burnout, estrés, renuncia) se invierte la escala.
    Devuelve: [{"module_id", "score"}]
    """
    buckets = defaultdict(list)
    inverts = {}
    qs = response.details.select_related("form_question__module")
    for d in qs:
        mod = d.form_question.module
        buckets[mod.id].append(d.value)
        inverts[mod.id] = mod.invert

    results = []
    for module_id, values in buckets.items():
        numeric = [v for v in values if v is not None]
        avg = sum(numeric) / len(numeric) if numeric else 3
        pct = ((avg - 1) / 4) * 100  # 1→0, 5→100
        if inverts[module_id]:
            pct = 100 - pct
        results.append({"module_id": module_id, "score": round(pct, 2)})
    return results
