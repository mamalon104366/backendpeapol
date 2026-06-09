import json, urllib.request as r, urllib.error

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
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or "null")


_, login = call("POST", "/api/auth/login/", body={"email": "marcela@andeslog.com", "password": "demo1234"})
t = login["access"]

print("== PREDICCIONES ==")
st, p = call("GET", "/api/predictions/", t)
m = p["meta"]
print(f"  modelo: {m.get('algorithm')} acc={m.get('test_accuracy')} auc={m.get('roc_auc')} n_train={m.get('n_train')}")
print("  drivers:", [(d["label"], d["importance"]) for d in m.get("importances", [])[:3]])
print(f"  {len(p['results'])} empleados. Top 3:")
for e in p["results"][:3]:
    print(f"     {e['employee_name']:<20} {e['area']:<12} {e['score']}% [{e['band']}] factor={e['top_factor']}")

print("\n== ALERTAS ==")
st, al = call("GET", "/api/alerts/", t)
for a in al:
    print(f"  [{a['level']}] {a['area']:<12} {a['title']} ({a['metric']})")

print("\n== CHAT (motor de reglas) ==")
for q in ["¿Por qué Soporte tiene tanto burnout?",
          "¿Quiénes están en riesgo de renunciar?",
          "¿Qué acción debería priorizar?"]:
    st, c = call("POST", "/api/chat/", t, body={"message": q})
    print(f"  Q: {q}\n  A[{c.get('source')}]: {c.get('answer')}\n")
print("status final:", st)
