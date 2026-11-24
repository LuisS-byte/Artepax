"""Create a local demo administrator without echoing or storing its password."""

import argparse
import getpass
import os
import re
import subprocess

import bcrypt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ci", action="store_true", help="Read credentials from the CI process environment.")
    args = parser.parse_args()

    if args.ci:
        email = os.environ.pop("ARTEPAX_ADMIN_EMAIL", "")
        password = os.environ.pop("ARTEPAX_ADMIN_PASSWORD", "")
    else:
        email = input("Correo del administrador: ").strip()
        password = getpass.getpass("Contraseña (mín. 12 bytes): ")
        if password != getpass.getpass("Confirma la contraseña: "):
            parser.error("Las contraseñas no coinciden.")

    email = email.lower()
    encoded = password.encode("utf-8")
    if len(email) > 100 or not re.fullmatch(r"[a-z0-9._+\-]+@[a-z0-9.\-]+\.[a-z]{2,}", email):
        parser.error("Introduce un correo válido.")
    if not 12 <= len(encoded) <= 72:
        parser.error("Usa de 12 a 72 bytes de contraseña.")

    password_hash = bcrypt.hashpw(encoded, bcrypt.gensalt(rounds=12, prefix=b"2a")).decode("ascii")
    sql = f"""BEGIN;
INSERT INTO public.usuario
    (usu_nombre, usu_correo, usu_password, usu_tipo_auth, estado, id_rol)
VALUES
    ('Administrador local', '{email}', '{password_hash}', 'LOCAL', true,
     (SELECT id_rol FROM public.rol WHERE rol_nombre = 'ADMIN'));
COMMIT;
"""
    result = subprocess.run(
        ["docker", "compose", "exec", "-T", "db", "sh", "-ec",
         'exec psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'],
        input=sql,
        text=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if result.returncode:
        parser.exit(1, "No se creó la cuenta. Comprueba que db esté iniciado, exista el rol ADMIN y el correo esté libre.\n")
    print("Administrador local creado.")


if __name__ == "__main__":
    main()
