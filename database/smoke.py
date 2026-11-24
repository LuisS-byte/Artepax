"""Exercise the local Compose demo with fictitious data."""

import datetime
import json
import os
import subprocess
import urllib.error
import urllib.request
import uuid


def request(method, path, payload=None, token=None, expected=(200,)):
    body = json.dumps(payload).encode() if payload is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    url = f"http://127.0.0.1:{os.environ.get('APP_PORT', '8080')}{path}"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, body, headers, method=method), timeout=30) as response:
            code, content = response.status, response.read()
    except urllib.error.HTTPError as error:
        code, content = error.code, b""
        error.close()
    if code not in expected:
        raise SystemExit(f"{method} {path}: HTTP {code}; esperado {expected}")
    if content and "application/json" in response.headers.get("Content-Type", ""):
        return json.loads(content)
    return None


def main():
    request("GET", "/v3/api-docs")
    request("GET", "/swagger-ui/index.html")
    request("GET", "/api/clientes", expected=(401, 403))
    request("POST", "/auth/login", {"email": "invalid@example.invalid", "password": "invalid"}, expected=(401,))
    login = {
        "email": os.environ["ARTEPAX_ADMIN_EMAIL"],
        "password": os.environ["ARTEPAX_ADMIN_PASSWORD"],
    }
    token = request("POST", "/auth/login", login)["token"]
    request("GET", "/api/usuarios/me", token=token)
    for path in ("/api/productos", "/api/decoraciones", "/api/tipos-evento", "/api/catalogos/metodos-pago"):
        if not request("GET", path, token=token):
            raise SystemExit(f"Catálogo vacío: {path}")
    suffix = uuid.uuid4().hex[:8]
    client = request("POST", "/api/clientes", {
        "nombre": "Cliente de prueba",
        "telefono": "0000-0000",
        "correo": f"cliente-{suffix}@example.invalid",
        "direccion": "Dirección ficticia",
    }, token, (200, 201))
    event = request("POST", "/api/eventos", {
        "clienteId": client["id"],
        "decoracionId": 1,
        "tipoEventoId": 1,
        "fecha": (datetime.date.today() + datetime.timedelta(days=30)).isoformat(),
        "horaInicio": "10:00",
        "horaFin": "12:00",
        "ubicacion": "Ubicación ficticia",
    }, token, (200, 201))
    event_id = int(event["id"])
    request("PATCH", f"/api/eventos/{event_id}/programar", token=token)
    sql = f"""SELECT count(*) FROM public.evento_producto ep
JOIN public.estado_reserva er USING (id_estado_reserva)
WHERE ep.id_evento = {event_id} AND er.estado_nombre = 'ACTIVA';"""
    reserved = subprocess.run(
        ["docker", "compose", "exec", "-T", "db", "sh", "-ec",
         'exec psql -X -At -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'],
        input=sql, text=True, capture_output=True, check=True,
    )
    if int(reserved.stdout.strip()) < 1:
        raise SystemExit("La programación no generó reservas activas.")
    payment = request("POST", "/api/pagos", {
        "eventoId": event_id, "monto": 1, "metodoPagoId": 1, "conceptoPagoId": 1,
    }, token, (200, 201))
    request("PATCH", f"/api/pagos/{int(payment['id'])}/confirmar", token=token)
    request("GET", f"/api/pagos/evento/{event_id}/resumen", token=token)
    print("Smoke local correcto: Swagger, acceso, catálogos, cliente, cotización, reserva y pago.")


if __name__ == "__main__":
    main()
