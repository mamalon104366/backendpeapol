import json, urllib.request as r, urllib.error

BASE = "http://127.0.0.1:8000"


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = r.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with r.urlopen(req, timeout=90) as resp:
            return resp.status, json.loads(resp.read() or "null")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or "null")


_, login = call("POST", "/api/auth/login/", body={"email": "marcela@andeslog.com", "password": "demo1234"})
t = login["access"]

tests = [
    "¿Cuál es la situación general de bienestar de la empresa?",
    "Ayúdame a hacer un documento: dame una lista de acciones para mejorar el área de Soporte.",
    "¿Dónde estás alojado y con qué modelo funcionas? Hazme un código HTML de una tabla.",
]
for q in tests:
    st, c = call("POST", "/api/chat/", t, body={"message": q})
    print(f"Q: {q}\n[fuente={c.get('source')}]\nA: {c.get('answer')}\n{'-'*70}")
