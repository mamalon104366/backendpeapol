"""Smoke test del API TalentMind (sin dependencias externas)."""
import json
import urllib.request as r

BASE = "http://127.0.0.1:8000"


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = r.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with r.urlopen(req) as resp:
            return resp.status, json.loads(resp.read() or "null")
    except urllib.error.HTTPError as e:  # type: ignore[name-defined]
        return e.code, json.loads(e.read() or "null")


import urllib.error  # noqa: E402

print("1) LOGIN")
st, login = call("POST", "/api/auth/login/", body={"email": "marcela@andeslog.com", "password": "demo1234"})
print("  ", st, "user:", login.get("user", {}).get("full_name"), "| role:", login.get("user", {}).get("role"))
token = login["access"]

print("2) CATÁLOGO /api/modules/")
st, mods = call("GET", "/api/modules/", token)
print("  ", st, "módulos:", len(mods), "| ej:", mods[0]["label"], f'({len(mods[0]["questions"])} preg.)')

print("3) CREAR FORMULARIO (auto-genera preguntas desde el banco)")
st, form = call("POST", "/api/forms/", token, body={
    "name": "Pulso de Bienestar · API",
    "description": "Creado por el smoke test",
    "ai_generated": True,
    "modules": ["burnout", "clima", "liderazgo"],
})
print("  ", st, "form id:", form.get("id"), "| preguntas:", len(form.get("questions", [])), "| módulos:", form.get("modules"))

print("4) RESPONDER (answers 1–5 por pregunta) → calcula indicadores")
answers = {}
for i, q in enumerate(form["questions"]):
    answers[str(q["id"])] = 2 if i % 7 == 0 else 4
st, resp = call("POST", "/api/responses/", token, body={
    "form": form["id"], "employee_name": "Test Bot", "area": "Soporte", "answers": answers,
})
print("  ", st, "response id:", resp.get("id"))
for s in resp.get("scores", []):
    print("      -", s["label"], f'{s["score"]}% [{s["band"]}]')

print("5) LISTAR FORMULARIOS /api/forms/")
st, forms = call("GET", "/api/forms/", token)
print("  ", st, "total formularios de la empresa:", forms.get("count"))

print("6) DASHBOARD /api/dashboard/summary/")
st, dash = call("GET", "/api/dashboard/summary/", token)
print("  ", st, "empresa:", dash.get("company"), "| empleados:", dash.get("employees"),
      "| respuestas:", dash.get("responses"), "| bienestar:", dash.get("overall_wellbeing"))
print("     indicadores:", [(i["label"], i["score"]) for i in dash.get("indicators", [])])

print("\n✅ SMOKE TEST OK" if st == 200 else "\n✖ revisar")
