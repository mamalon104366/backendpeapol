from django.db import models

from apps.companies.models import Company, Employee


# ---------- Catálogo global ----------
class Module(models.Model):
    id = models.CharField(primary_key=True, max_length=40)  # slug: burnout, clima…
    label = models.CharField(max_length=80)
    description = models.TextField(blank=True)
    invert = models.BooleanField(default=False)  # True → mayor puntaje = peor
    color = models.CharField(max_length=16, blank=True)
    icon = models.CharField(max_length=40, blank=True)

    def __str__(self):
        return self.label


class ModuleQuestion(models.Model):
    id = models.CharField(primary_key=True, max_length=40)  # slug: bo1, cl2…
    module = models.ForeignKey(Module, on_delete=models.CASCADE, related_name="questions")
    text = models.TextField()
    ord = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["ord"]

    def __str__(self):
        return self.text


# ---------- Formularios ----------
class Form(models.Model):
    class Status(models.TextChoices):
        DRAFT = "borrador", "Borrador"
        ACTIVE = "activo", "Activo"
        CLOSED = "cerrado", "Cerrado"

    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name="forms")
    created_by = models.ForeignKey(
        "accounts.User",
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="created_forms",
    )
    name = models.CharField(max_length=200)
    description = models.TextField(blank=True)
    status = models.CharField(max_length=12, choices=Status.choices, default=Status.ACTIVE)
    ai_generated = models.BooleanField(default=False)
    scale_max = models.PositiveSmallIntegerField(default=5)
    modules = models.ManyToManyField(Module, related_name="forms", blank=True)
    deadline = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return self.name


class FormQuestion(models.Model):
    class QuestionType(models.TextChoices):
        SCALE = "scale", "Escala 1-5"
        BOOLEAN = "boolean", "Verdadero/Falso"
        MULTIPLE = "multiple", "Opción múltiple"
        OPEN = "open", "Abierta"

    form = models.ForeignKey(Form, on_delete=models.CASCADE, related_name="questions")
    module = models.ForeignKey(Module, on_delete=models.PROTECT, related_name="form_questions")
    text = models.TextField()
    question_type = models.CharField(
        max_length=20,
        choices=QuestionType.choices,
        default=QuestionType.SCALE,
    )
    options = models.JSONField(null=True, blank=True)
    ord = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["ord", "id"]

    def __str__(self):
        return self.text


# ---------- Respuestas ----------
class Response(models.Model):
    form = models.ForeignKey(Form, on_delete=models.CASCADE, related_name="responses")
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name="responses")
    employee = models.ForeignKey(
        Employee, null=True, blank=True, on_delete=models.SET_NULL, related_name="responses"
    )
    employee_name = models.CharField(max_length=160, blank=True)
    area = models.CharField(max_length=120, blank=True)
    submitted_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-submitted_at"]


class ResponseDetail(models.Model):
    response = models.ForeignKey(Response, on_delete=models.CASCADE, related_name="details")
    form_question = models.ForeignKey(FormQuestion, on_delete=models.CASCADE)
    value = models.PositiveSmallIntegerField(null=True, blank=True)  # 1–5
    text_value = models.TextField(blank=True)


class FormRecipient(models.Model):
    class Status(models.TextChoices):
        PENDING = "pending", "Pendiente"
        COMPLETED = "completed", "Completado"

    form = models.ForeignKey(Form, on_delete=models.CASCADE, related_name="recipients")
    employee = models.ForeignKey(
        Employee, on_delete=models.CASCADE, related_name="form_recipients"
    )
    status = models.CharField(max_length=12, choices=Status.choices, default=Status.PENDING)
    assigned_at = models.DateTimeField(auto_now_add=True)
    completed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        unique_together = ("form", "employee")
        ordering = ["employee__department__name", "employee__role_title", "employee__full_name"]

    def __str__(self):
        return f"{self.form_id} · {self.employee_id} · {self.status}"


class IndicatorScore(models.Model):
    response = models.ForeignKey(Response, on_delete=models.CASCADE, related_name="scores")
    module = models.ForeignKey(Module, on_delete=models.CASCADE)
    score = models.DecimalField(max_digits=5, decimal_places=2)  # salud 0–100


# ---------- Inteligencia (alimenta el dashboard / M3) ----------
class Alert(models.Model):
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name="alerts")
    level = models.CharField(max_length=10)  # risk | warn
    area = models.CharField(max_length=120, blank=True)
    title = models.CharField(max_length=160)
    detail = models.TextField(blank=True)
    metric = models.CharField(max_length=20, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]


class Prediction(models.Model):
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name="predictions")
    employee = models.ForeignKey(Employee, on_delete=models.CASCADE, related_name="predictions")
    kind = models.CharField(max_length=20, default="renuncia")  # renuncia | burnout
    score = models.DecimalField(max_digits=5, decimal_places=2)  # probabilidad 0–100
    top_factor = models.CharField(max_length=120, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-score"]


class AiConversation(models.Model):
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name="conversations")
    question = models.TextField()
    answer = models.TextField(blank=True)
    source = models.CharField(max_length=20, default="rules")  # gemini | rules
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["created_at"]
