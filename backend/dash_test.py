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


st, login = call("POST", "/api/auth/login/", body={"email": "marcela@andeslog.com", "password": "demo1234"})
token = login["access"]
st, d = call("GET", "/api/dashboard/summary/", token)
print("status:", st)
print("empresa:", d["company"], "| empleados:", d["employees"], "| respuestas:", d["responses"], "| participación:", d["response_rate"])
print("bienestar global:", d["overall_wellbeing"])
print("\nKPIs:")
for k in d["kpis"]:
    print(f"   {k['label']}: {k['value']}%  (Δ {k['delta']})  [{'invertido' if k['invert'] else 'normal'}]")
print("\nPor área (burnout raw):")
for a in d["by_area"]:
    print(f"   {a['area']:<14} burnout {a['burnout']}%  bienestar {a['bienestar']}%  ({a['headcount']} pers.)")
print("\nTendencia:", [(t["mes"], t["bienestar"], t["riesgo"]) for t in d["trend"]])
print("\nAlertas:", [(a["level"], a["area"], a["metric"]) for a in d["alerts"]])
print("\nEn riesgo (top 5):")
for e in d["at_risk"]:
    print(f"   {e['name']:<20} {e['area']:<12} riesgo {e['risk']}%")
print("\ndistribución:", d["distribution"])
print("indicadores:", [(i["label"], i["score"]) for i in d["indicators"]])
