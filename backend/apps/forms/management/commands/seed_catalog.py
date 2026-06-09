"""
Siembra el catálogo de módulos/preguntas + una empresa demo con usuario RH,
departamentos y empleados sintéticos. Idempotente.

    python manage.py seed_catalog

Refleja src/data/questionBank.ts y db/seed.mjs del frontend.
"""
from datetime import date

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand
from django.db import transaction

from apps.companies.models import Company, Department, Employee
from apps.forms.models import Module, ModuleQuestion

User = get_user_model()

MODULES = [
    ("burnout", "Burnout", True, "#fb6f6f", "Flame", "Agotamiento físico y emocional por el trabajo.", [
        ("bo1", "Me siento agotado/a al finalizar mi jornada laboral."),
        ("bo2", "Siento que mi carga de trabajo es excesiva."),
        ("bo3", "Me cuesta desconectarme del trabajo en mi tiempo libre."),
        ("bo4", "Siento presión constante para cumplir con mis tareas."),
        ("bo5", "Me siento emocionalmente vacío/a por mi trabajo."),
    ]),
    ("clima", "Clima laboral", False, "#2dd4bf", "CloudSun", "Ambiente, respeto y comunicación en el equipo.", [
        ("cl1", "Me siento cómodo/a trabajando con mis compañeros."),
        ("cl2", "Existe respeto entre las personas de mi área."),
        ("cl3", "La comunicación interna es clara y oportuna."),
        ("cl4", "Puedo expresar mis ideas sin temor a represalias."),
        ("cl5", "El ambiente de mi área es agradable."),
    ]),
    ("estres", "Estrés", True, "#fbbf4c", "Activity", "Nivel de tensión y presión percibida.", [
        ("es1", "Frecuentemente me siento tenso/a o nervioso/a en el trabajo."),
        ("es2", "Los plazos de entrega me generan mucha ansiedad."),
        ("es3", "Tengo dificultad para concentrarme por la presión."),
        ("es4", "El estrés del trabajo afecta mi descanso."),
    ]),
    ("motivacion", "Motivación", False, "#8b9bff", "Rocket", "Energía e ilusión por las tareas diarias.", [
        ("mo1", "Me siento motivado/a para dar lo mejor de mí cada día."),
        ("mo2", "Mi trabajo tiene un propósito que me importa."),
        ("mo3", "Me entusiasman los proyectos en los que participo."),
        ("mo4", "Siento que mi esfuerzo vale la pena."),
    ]),
    ("liderazgo", "Liderazgo", False, "#5eead4", "Compass", "Calidad del acompañamiento de jefaturas.", [
        ("li1", "Mi jefe/a escucha mis opiniones y sugerencias."),
        ("li2", "Recibo retroalimentación útil sobre mi desempeño."),
        ("li3", "Tengo apoyo de mi líder cuando lo necesito."),
        ("li4", "Mi líder comunica con claridad lo que se espera de mí."),
        ("li5", "Confío en las decisiones de mi jefatura."),
    ]),
    ("satisfaccion", "Satisfacción laboral", False, "#43e0a3", "Smile", "Conformidad general con el puesto.", [
        ("sa1", "En general, estoy satisfecho/a con mi trabajo."),
        ("sa2", "Mi salario es justo para las funciones que realizo."),
        ("sa3", "Las condiciones de mi puesto son adecuadas."),
        ("sa4", "Recomendaría esta empresa como un buen lugar para trabajar."),
    ]),
    ("renuncia", "Riesgo de renuncia", True, "#f2545b", "DoorOpen", "Intención de permanencia en la empresa.", [
        ("rn1", "He pensado en buscar trabajo en otra empresa."),
        ("rn2", "Si me ofrecieran un puesto similar, lo consideraría seriamente."),
        ("rn3", "No me veo trabajando aquí dentro de un año."),
        ("rn4", "Siento que mi crecimiento aquí está estancado."),
    ]),
    ("equipo", "Trabajo en equipo", False, "#2dd4bf", "Users", "Colaboración y cohesión del grupo.", [
        ("eq1", "Mi equipo colabora bien para lograr objetivos comunes."),
        ("eq2", "Puedo contar con mis compañeros cuando necesito ayuda."),
        ("eq3", "Los conflictos en mi equipo se resuelven de forma sana."),
        ("eq4", "Compartimos información de forma abierta."),
    ]),
    ("productividad", "Productividad", False, "#5eead4", "Gauge", "Capacidad de cumplir y rendir.", [
        ("pr1", "Cuento con las herramientas necesarias para hacer bien mi trabajo."),
        ("pr2", "Logro cumplir mis objetivos en los tiempos establecidos."),
        ("pr3", "Las reuniones de mi área son productivas."),
        ("pr4", "Mis tareas están bien organizadas y priorizadas."),
    ]),
    ("capacitacion", "Capacitación", False, "#8b9bff", "GraduationCap", "Oportunidades de aprendizaje y desarrollo.", [
        ("ca1", "La empresa me brinda oportunidades de capacitación."),
        ("ca2", "Tengo un plan de desarrollo profesional claro."),
        ("ca3", "Aprendo cosas nuevas que mejoran mis habilidades."),
        ("ca4", "Se reconoce mi crecimiento dentro de la empresa."),
    ]),
    ("personalizado", "Análisis personalizado", False, "#a855f7", "Sparkles",
     "Preguntas generadas por IA sobre el tema que definas.", []),
]

DEPARTMENTS = ["Soporte", "Ventas", "Operaciones", "Tecnología", "Finanzas", "Marketing", "RR.HH."]
FIRST = ["Juan", "Lucía", "Diego", "Camila", "Tomás", "Sofía", "Mateo", "Valentina", "Andrés",
         "Daniela", "Pablo", "Renata", "Iván", "Carla", "Bruno", "Elena", "Hugo", "Paula",
         "Nicolás", "Marta", "Felipe", "Lorena", "Sergio", "Gabriela"]
LAST = ["Marroquín", "Ferreira", "Salas", "Ortiz", "Rivero", "Castro", "Núñez", "Vega",
        "Mora", "Pérez", "Lara", "Soto"]
TITLES = ["Analista", "Coordinador/a", "Ejecutivo/a", "Especialista", "Asistente", "Líder de equipo"]


class Command(BaseCommand):
    help = "Siembra catálogo de módulos + empresa demo (idempotente)."

    @transaction.atomic
    def handle(self, *args, **opts):
        self.stdout.write("→ Sembrando catálogo de módulos…")
        for mid, label, invert, color, icon, desc, qs in MODULES:
            Module.objects.update_or_create(
                id=mid,
                defaults=dict(label=label, invert=invert, color=color, icon=icon, description=desc),
            )
            for i, (qid, text) in enumerate(qs):
                ModuleQuestion.objects.update_or_create(
                    id=qid, defaults=dict(module_id=mid, text=text, ord=i)
                )

        self.stdout.write("→ Empresa + departamentos…")
        company, _ = Company.objects.get_or_create(
            name="Andes Logística", defaults=dict(plan="Business")
        )
        # Los usuarios (profesor + estudiantes) los crea seed_roster.
        for name in DEPARTMENTS:
            Department.objects.get_or_create(company=company, name=name)

        self.stdout.write(self.style.SUCCESS(
            f"✅ Listo · módulos={Module.objects.count()} "
            f"preguntas={ModuleQuestion.objects.count()} "
            f"empresas={Company.objects.count()} "
            f"empleados={Employee.objects.count()}"
        ))
