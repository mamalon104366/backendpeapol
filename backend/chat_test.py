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

preguntas = [
    "hola",
    "¿qué puedes hacer?",
    "¿qué área está peor?",
    "¿cuál es la mejor área?",
    "háblame de Soporte",
    "¿cómo está el liderazgo?",
    "¿cómo va la motivación?",
    "¿quién se va a ir?",
    "¿cómo está Lorena?",
    "¿cómo va la tendencia?",
    "¿qué me recomiendas hacer?",
    "¿cuántos empleados tenemos?",
    "¿qué hora es?",
]
for p in preguntas:
    _, c = call("POST", "/api/chat/", t, body={"message": p})
    print(f"Q: {p}\nA: {c.get('answer')}\n")
