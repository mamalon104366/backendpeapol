"""
Carga el roster real de empleados (simulación de clase): cada persona con su
rol, su área y una cuenta de acceso @prueba.com. Crea también al profesor
Alexander Amaris como Administrador de RR.HH.

    python manage.py seed_roster --reset    # reemplaza los empleados sintéticos

Después conviene correr: seed_responses --reset  y  train_predictions
"""
import random
import unicodedata
from datetime import date, timedelta

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand
from django.db import transaction

from apps.companies.models import Company, Department, Employee

User = get_user_model()

NAMES = [
    "CABRALES BARBARAN, JUAN FELIPE",
    "CAMPO CABALLERO, VÍCTOR EDUARDO",
    "CAMPOS LOPEZ, ZURY",
    "GOMEZ AVENDANO, JAMES ANDRES",
    "GUILLEN DONADO, ANDRES MAURICIO",
    "JIMENEZ GÓMEZ, JUAN CAMILO",
    "MACIAS MARTINEZ, JUAN JOSE",
    "MARIN LOBO, LUZDEY NATALIA",
    "MARTINEZ PALOMINO, JESUS ALBERTO",
    "MARTINEZ ZORA, ERICK JOEL",
    "MENESES RUEDA, CRISTIAN DAVID",
    "MERLANO QUINTANA, ESTEBAN DAVID",
    "NAVARRO SIMANCA, NICOLL",
    "PACHECO MONCADA, SAID ALEXANDER",
    "PENA SOLANO, ELVIS",
    "RICAURTE MORA, ALEXANDER",
    "ROBLES SERRANO, YELBERTH ANDRES",
    "TORRES RODRIGUEZ, YULY VANESSA",
    "TRUJILLO GUERRA, KEINER SANTIAGO",
    "VELANDIA BANDERA, BRANDO",
    "ANTOLINEZ SUAREZ, JUAN ESTEBAN",
    "BADILLO ORTEGA, NESTOR J IVAN",
    "CHAVEZ VILLAFAÑE, HADIK ANDRES",
    "TARAZONA CASTRO, ELIEL JOSUE",
    "TOLOZA MENDOZA, LUIS ALEJANDRO",
]

DEPT_ROLES = {
    "Soporte": ["Agente de Soporte N1", "Agente de Soporte N2", "Coordinador de Soporte"],
    "Ventas": ["Ejecutivo de Ventas", "Asesor Comercial", "Líder Comercial"],
    "Operaciones": ["Analista de Operaciones", "Auxiliar Operativo", "Coordinador de Operaciones"],
    "Tecnología": ["Desarrollador", "Analista QA", "Soporte TI"],
    "Finanzas": ["Analista Financiero", "Auxiliar Contable", "Contador"],
    "Marketing": ["Analista de Marketing", "Community Manager", "Diseñador Gráfico"],
    "RR.HH.": ["Analista de RR.HH.", "Reclutador", "Coordinador de RR.HH."],
}
DEPTS = list(DEPT_ROLES.keys())


def _norm(s):
    s = "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")
    return s.lower().replace("ñ", "n").replace("'", "")


def _parse(line):
    surnames, givens = [p.strip() for p in line.split(",")]
    g, s = givens.split(), surnames.split()
    display = " ".join(w.capitalize() for w in (g + s))
    local = f"{_norm(g[0])}.{_norm(s[0])}"
    return display, local


class Command(BaseCommand):
    help = "Carga el roster real de empleados (clase) con rol, área y cuenta @prueba.com."

    def add_arguments(self, parser):
        parser.add_argument("--reset", action="store_true", help="Borra los empleados previos.")

    @transaction.atomic
    def handle(self, *args, **opts):
        company = Company.objects.filter(name="Andes Logística").first()
        if not company:
            self.stderr.write("✖ Falta la empresa demo (corre seed_catalog).")
            return

        depts = {d: Department.objects.get_or_create(company=company, name=d)[0] for d in DEPTS}

        if opts["reset"]:
            emps = Employee.objects.filter(company=company)
            for e in emps.select_related("user"):
                if e.user and e.user.role == "employee":
                    e.user.delete()  # OneToOne → también borra la fila employee
            Employee.objects.filter(company=company).delete()

        # Profesor / Admin RR.HH.
        if not User.objects.filter(email__iexact="alexander.amaris@prueba.com").exists():
            User.objects.create_user(
                email="alexander.amaris@prueba.com", password="people123",
                full_name="Alexander Amaris", role=User.Role.HR_ADMIN, company=company,
            )

        used, created = set(), 0
        for i, line in enumerate(NAMES):
            display, local = _parse(line)
            email = f"{local}@prueba.com"
            k = 2
            while email in used or User.objects.filter(email__iexact=email).exists():
                email = f"{local}{k}@prueba.com"
                k += 1
            used.add(email)

            dept_name = DEPTS[i % len(DEPTS)]
            roles = DEPT_ROLES[dept_name]
            role = roles[(i // len(DEPTS)) % len(roles)]

            # Los de RR.HH. tienen acceso al panel administrativo; el resto son empleados.
            user_role = User.Role.HR_ADMIN if dept_name == "RR.HH." else User.Role.EMPLOYEE
            user = User.objects.create_user(
                email=email, password="people123", full_name=display,
                role=user_role, company=company,
            )
            hire = date.today() - timedelta(days=random.randint(90, 2100))
            Employee.objects.create(
                company=company, department=depts[dept_name], user=user,
                full_name=display, role_title=role, hire_date=hire,
            )
            created += 1

        self.stdout.write(self.style.SUCCESS(
            f"✅ {created} empleados cargados con rol+área+cuenta (@prueba.com / pass people123). "
            f"Profesor Alexander Amaris = Admin RR.HH. (alexander.amaris@prueba.com / people123)."
        ))
