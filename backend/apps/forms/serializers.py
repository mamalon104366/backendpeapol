from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from apps.companies.models import Employee

from .models import (
    Alert,
    Form,
    FormQuestion,
    FormRecipient,
    IndicatorScore,
    Module,
    ModuleQuestion,
    Prediction,
    Response,
    ResponseDetail,
)
from .scoring import band, compute_module_scores


QUESTION_TYPES = {choice[0] for choice in FormQuestion.QuestionType.choices}


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    text = str(value).strip().lower()
    return text in {"1", "true", "t", "yes", "si", "sí", "verdadero", "v"}


def _option_display(options, raw):
    if isinstance(raw, dict):
        raw = raw.get("label") or raw.get("text") or raw.get("value")
    raw_text = str(raw).strip()
    if not raw_text:
        return ""
    for option in options or []:
        label = str(option.get("label", "")).strip()
        text = str(option.get("text", "")).strip()
        if raw_text.lower() in {label.lower(), text.lower()}:
            if label and text:
                return f"{label}. {text}"
            return text or label
    return raw_text


def _question_answer_payload(question: FormQuestion, raw):
    qtype = question.question_type
    if qtype == FormQuestion.QuestionType.SCALE:
        try:
            value = int(raw)
        except (TypeError, ValueError):
            raise serializers.ValidationError(
                {str(question.id): "Las preguntas de escala requieren un valor numérico."}
            )
        if value < 1 or value > 5:
            raise serializers.ValidationError(
                {str(question.id): "La escala debe estar entre 1 y 5."}
            )
        return value, ""

    if qtype == FormQuestion.QuestionType.BOOLEAN:
        return None, "Verdadero" if _truthy(raw) else "Falso"

    if qtype == FormQuestion.QuestionType.MULTIPLE:
        return None, _option_display(question.options, raw)

    return None, str(raw).strip()


# ---------- Catálogo ----------
class ModuleQuestionSerializer(serializers.ModelSerializer):
    class Meta:
        model = ModuleQuestion
        fields = ["id", "text", "ord"]


class ModuleSerializer(serializers.ModelSerializer):
    questions = ModuleQuestionSerializer(many=True, read_only=True)

    class Meta:
        model = Module
        fields = ["id", "label", "description", "invert", "color", "icon", "questions"]


# ---------- Formularios ----------
class FormQuestionSerializer(serializers.ModelSerializer):
    class Meta:
        model = FormQuestion
        fields = ["id", "module", "text", "question_type", "options", "ord"]


class FormRecipientSerializer(serializers.ModelSerializer):
    employee_id = serializers.IntegerField(source="employee.id", read_only=True)
    employee_name = serializers.CharField(source="employee.full_name", read_only=True)
    area = serializers.SerializerMethodField()
    role = serializers.SerializerMethodField()
    state = serializers.SerializerMethodField()
    state_label = serializers.SerializerMethodField()
    tone = serializers.SerializerMethodField()

    class Meta:
        model = FormRecipient
        fields = [
            "employee_id",
            "employee_name",
            "area",
            "role",
            "status",
            "state",
            "state_label",
            "tone",
            "assigned_at",
            "completed_at",
        ]

    def _state(self, obj):
        deadline = obj.form.deadline
        now = timezone.now()
        if obj.status == FormRecipient.Status.COMPLETED:
            if deadline and obj.completed_at and obj.completed_at > deadline:
                return "completed_late"
            return "completed"
        if deadline and now > deadline:
            return "overdue"
        return "pending"

    def get_state(self, obj):
        return self._state(obj)

    def get_area(self, obj):
        department = getattr(obj.employee, "department", None)
        return department.name if department else "Sin área"

    def get_role(self, obj):
        return obj.employee.role_title or "Sin cargo"

    def get_state_label(self, obj):
        return {
            "completed": "Completado",
            "completed_late": "Completado tarde",
            "pending": "Pendiente",
            "overdue": "Vencido",
        }[self._state(obj)]

    def get_tone(self, obj):
        return {
            "completed": "good",
            "completed_late": "warn",
            "pending": "warn",
            "overdue": "risk",
        }[self._state(obj)]


class FormSerializer(serializers.ModelSerializer):
    questions = FormQuestionSerializer(many=True, read_only=True)
    modules = serializers.SlugRelatedField(many=True, slug_field="id", read_only=True)
    response_count = serializers.IntegerField(read_only=True)
    recipient_count = serializers.IntegerField(read_only=True)
    completed_count = serializers.IntegerField(read_only=True)
    pending_count = serializers.IntegerField(read_only=True)
    overdue_count = serializers.SerializerMethodField()
    created_by_name = serializers.SerializerMethodField()

    class Meta:
        model = Form
        fields = [
            "id",
            "name",
            "description",
            "status",
            "ai_generated",
            "scale_max",
            "modules",
            "questions",
            "response_count",
            "recipient_count",
            "completed_count",
            "pending_count",
            "overdue_count",
            "deadline",
            "created_by_name",
            "created_at",
        ]

    def get_overdue_count(self, obj):
        deadline = obj.deadline
        if not deadline or deadline >= timezone.now():
            return 0
        return getattr(obj, "pending_count", 0)

    def get_created_by_name(self, obj):
        return getattr(obj.created_by, "full_name", "")


class QuestionOptionInput(serializers.Serializer):
    label = serializers.CharField(max_length=8)
    text = serializers.CharField()


class FormQuestionInput(serializers.Serializer):
    module = serializers.PrimaryKeyRelatedField(queryset=Module.objects.all())
    text = serializers.CharField()
    question_type = serializers.ChoiceField(choices=FormQuestion.QuestionType.choices, default=FormQuestion.QuestionType.SCALE)
    options = QuestionOptionInput(many=True, required=False)

    def validate(self, attrs):
        qtype = attrs.get("question_type", FormQuestion.QuestionType.SCALE)
        options = attrs.get("options") or []
        if qtype == FormQuestion.QuestionType.MULTIPLE:
            if len(options) != 4:
                raise serializers.ValidationError(
                    {"options": "Las preguntas de opción múltiple deben tener exactamente 4 opciones."}
                )
        else:
            attrs["options"] = None
        return attrs


class FormCreateSerializer(serializers.Serializer):
    name = serializers.CharField(max_length=200)
    description = serializers.CharField(required=False, allow_blank=True, default="")
    ai_generated = serializers.BooleanField(default=False)
    scale_max = serializers.IntegerField(default=5)
    status = serializers.ChoiceField(choices=Form.Status.choices, default=Form.Status.ACTIVE)
    deadline = serializers.DateTimeField(required=False, allow_null=True)
    modules = serializers.PrimaryKeyRelatedField(many=True, queryset=Module.objects.all())
    recipient_ids = serializers.ListField(
        child=serializers.IntegerField(), required=False, allow_empty=True
    )
    questions = FormQuestionInput(many=True, required=False)

    def _resolve_recipients(self, company, recipient_ids):
        if not company:
            return []
        if not recipient_ids:
            return list(Employee.objects.filter(company=company).select_related("department"))
        employees = list(
            Employee.objects.filter(company=company, id__in=recipient_ids).select_related("department")
        )
        found_ids = {e.id for e in employees}
        missing = [emp_id for emp_id in recipient_ids if emp_id not in found_ids]
        if missing:
            raise serializers.ValidationError(
                {"recipient_ids": f"Los empleados {missing} no pertenecen a la empresa."}
            )
        return employees

    @transaction.atomic
    def create(self, data):
        company = data["company"]
        if not company:
            raise serializers.ValidationError({"company": "No se pudo determinar la empresa del formulario."})

        modules = list(data["modules"])
        questions = data.get("questions") or []
        recipient_ids = data.get("recipient_ids") or []

        form = Form.objects.create(
            company=company,
            created_by=data.get("created_by"),
            name=data["name"],
            description=data.get("description", ""),
            ai_generated=data.get("ai_generated", False),
            scale_max=data.get("scale_max", 5),
            status=data.get("status", Form.Status.ACTIVE),
            deadline=data.get("deadline"),
        )

        question_modules = []
        if questions:
            for i, q in enumerate(questions):
                FormQuestion.objects.create(
                    form=form,
                    module=q["module"],
                    text=q["text"],
                    question_type=q.get("question_type", FormQuestion.QuestionType.SCALE),
                    options=q.get("options"),
                    ord=i,
                )
                question_modules.append(q["module"])
        else:
            ord_ = 0
            for module in modules:
                for mq in module.questions.all():
                    FormQuestion.objects.create(
                        form=form,
                        module=module,
                        text=mq.text,
                        question_type=FormQuestion.QuestionType.SCALE,
                        options=None,
                        ord=ord_,
                    )
                    ord_ += 1
                question_modules.append(module)

        module_ids = {module.id for module in modules} | {module.id for module in question_modules}
        if module_ids:
            form.modules.set(Module.objects.filter(id__in=module_ids))

        recipients = self._resolve_recipients(company, recipient_ids)
        if recipients:
            FormRecipient.objects.bulk_create(
                [FormRecipient(form=form, employee=employee) for employee in recipients],
                ignore_conflicts=True,
            )

        return form

    def to_representation(self, instance):
        return FormSerializer(instance, context=self.context).data


# ---------- Respuestas ----------
class IndicatorScoreSerializer(serializers.ModelSerializer):
    label = serializers.CharField(source="module.label", read_only=True)
    band = serializers.SerializerMethodField()

    class Meta:
        model = IndicatorScore
        fields = ["module", "label", "score", "band"]

    def get_band(self, obj):
        return band(float(obj.score))


class ResponseDetailSerializer(serializers.ModelSerializer):
    question_text = serializers.CharField(source="form_question.text", read_only=True)
    question_type = serializers.CharField(source="form_question.question_type", read_only=True)

    class Meta:
        model = ResponseDetail
        fields = ["id", "form_question", "question_text", "question_type", "value", "text_value"]


class ResponseSerializer(serializers.ModelSerializer):
    scores = IndicatorScoreSerializer(many=True, read_only=True)
    details = ResponseDetailSerializer(many=True, read_only=True)

    class Meta:
        model = Response
        fields = ["id", "form", "employee_name", "area", "submitted_at", "details", "scores"]


class ResponseCreateSerializer(serializers.Serializer):
    form = serializers.PrimaryKeyRelatedField(queryset=Form.objects.select_related("company"))
    employee_id = serializers.IntegerField(required=False, allow_null=True)
    employee_name = serializers.CharField(required=False, allow_blank=True, default="")
    area = serializers.CharField(required=False, allow_blank=True, default="")
    answers = serializers.DictField(child=serializers.JSONField())

    def validate(self, attrs):
        request = self.context["request"]
        user = request.user
        form = attrs["form"]
        user_company = getattr(user, "company", None)
        if getattr(user, "role", None) != "super_admin" and user_company and form.company_id != user_company.id:
            raise serializers.ValidationError({"form": "El formulario no pertenece a tu empresa."})

        employee = None
        employee_id = attrs.get("employee_id")
        if employee_id is not None:
            if getattr(user, "role", None) not in {"hr_admin", "super_admin"}:
                user_employee = getattr(user, "employee", None)
                if not user_employee or user_employee.id != employee_id:
                    raise serializers.ValidationError(
                        {"employee_id": "No puedes responder en nombre de otro empleado."}
                    )
            employee = (
                Employee.objects.select_related("department")
                .filter(company=form.company, id=employee_id)
                .first()
            )
            if not employee:
                raise serializers.ValidationError({"employee_id": "Empleado no válido para esta empresa."})
        else:
            employee = getattr(user, "employee", None)

        if getattr(user, "role", None) not in {"hr_admin", "super_admin"} and not employee:
            raise serializers.ValidationError(
                {"employee_id": "Tu usuario no está vinculado a un empleado."}
            )

        if employee and getattr(user, "role", None) not in {"hr_admin", "super_admin"}:
            if not form.recipients.filter(employee=employee).exists():
                raise serializers.ValidationError({"form": "No tienes acceso a este formulario."})

        attrs["employee_obj"] = employee
        return attrs

    @transaction.atomic
    def create(self, data):
        form = data["form"]
        company = form.company
        employee = data.get("employee_obj")
        answers = data.get("answers") or {}

        resp = Response.objects.create(
            form=form,
            company=company,
            employee=employee,
            employee_name=(data.get("employee_name") or getattr(employee, "full_name", "")),
            area=(
                data.get("area")
                or (
                    employee.department.name
                    if getattr(employee, "department", None)
                    else ""
                )
            ),
        )

        fq_map = {str(q.id): q for q in form.questions.all()}
        for qid, raw in answers.items():
            q = fq_map.get(str(qid))
            if not q:
                continue
            value, text_value = _question_answer_payload(q, raw)
            ResponseDetail.objects.create(
                response=resp,
                form_question=q,
                value=value,
                text_value=text_value,
            )

        for score in compute_module_scores(resp):
            IndicatorScore.objects.create(
                response=resp, module_id=score["module_id"], score=score["score"]
            )

        if employee:
            FormRecipient.objects.filter(form=form, employee=employee).update(
                status=FormRecipient.Status.COMPLETED,
                completed_at=timezone.now(),
            )

        return resp

    def to_representation(self, instance):
        return ResponseSerializer(instance, context=self.context).data


# ---------- Inteligencia (M3) ----------
class PredictionSerializer(serializers.ModelSerializer):
    employee_name = serializers.CharField(source="employee.full_name", read_only=True)
    area = serializers.SerializerMethodField()
    role = serializers.SerializerMethodField()
    band = serializers.SerializerMethodField()

    class Meta:
        model = Prediction
        fields = ["id", "employee_name", "area", "role", "kind", "score", "top_factor", "band", "created_at"]

    def get_area(self, obj):
        department = getattr(obj.employee, "department", None)
        return department.name if department else "—"

    def get_role(self, obj):
        return obj.employee.role_title or "—"

    def get_band(self, obj):
        return band(100 - float(obj.score))  # score alto = más riesgo


class AlertSerializer(serializers.ModelSerializer):
    class Meta:
        model = Alert
        fields = ["id", "level", "area", "title", "detail", "metric", "created_at"]


class MyFormSerializer(serializers.ModelSerializer):
    """Un formulario asignado, desde el punto de vista del empleado."""

    form_id = serializers.IntegerField(source="form.id", read_only=True)
    name = serializers.CharField(source="form.name", read_only=True)
    description = serializers.CharField(source="form.description", read_only=True)
    question_count = serializers.SerializerMethodField()
    deadline = serializers.DateTimeField(source="form.deadline", read_only=True)
    form_status = serializers.CharField(source="form.status", read_only=True)
    ai_generated = serializers.BooleanField(source="form.ai_generated", read_only=True)
    state = serializers.SerializerMethodField()
    state_label = serializers.SerializerMethodField()
    tone = serializers.SerializerMethodField()

    class Meta:
        model = FormRecipient
        fields = [
            "form_id", "name", "description", "question_count", "deadline", "form_status",
            "ai_generated", "status", "state", "state_label", "tone", "completed_at", "assigned_at",
        ]

    def _state(self, obj):
        deadline = obj.form.deadline
        now = timezone.now()
        if obj.status == FormRecipient.Status.COMPLETED:
            if deadline and obj.completed_at and obj.completed_at > deadline:
                return "completed_late"
            return "completed"
        if deadline and now > deadline:
            return "overdue"
        return "pending"

    def get_question_count(self, obj):
        return obj.form.questions.count()

    def get_state(self, obj):
        return self._state(obj)

    def get_state_label(self, obj):
        return {
            "completed": "Completado", "completed_late": "Completado tarde",
            "pending": "Pendiente", "overdue": "Vencido",
        }[self._state(obj)]

    def get_tone(self, obj):
        return {
            "completed": "good", "completed_late": "warn",
            "pending": "warn", "overdue": "risk",
        }[self._state(obj)]
