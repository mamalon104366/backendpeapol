import json
from pathlib import Path

from django.conf import settings
from django.db.models import Count, Q
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied
from rest_framework import permissions, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response as ApiResponse
from rest_framework.views import APIView

from apps.common import IsHRStaff, TenantScopedViewSet
from apps.companies.serializers import MeProfileSerializer

from .models import Alert, Form, FormRecipient, Module, Prediction, Response
from .serializers import (
    AlertSerializer,
    FormCreateSerializer,
    FormRecipientSerializer,
    FormSerializer,
    ModuleSerializer,
    MyFormSerializer,
    PredictionSerializer,
    ResponseCreateSerializer,
    ResponseSerializer,
)
from .video_processor import process_video_and_generate_form


class MeProfileView(APIView):
    """Perfil propio del empleado autenticado (GET / PATCH self-service)."""

    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        emp = getattr(request.user, "employee", None)
        if not emp:
            return ApiResponse({"detail": "Tu usuario no está vinculado a un empleado."}, status=404)
        return ApiResponse(MeProfileSerializer(emp).data)

    def patch(self, request):
        emp = getattr(request.user, "employee", None)
        if not emp:
            return ApiResponse({"detail": "Tu usuario no está vinculado a un empleado."}, status=404)
        ser = MeProfileSerializer(emp, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return ApiResponse(ser.data)


class MyFormsView(APIView):
    """Encuestas asignadas al empleado (pendientes y completadas)."""

    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        emp = getattr(request.user, "employee", None)
        if not emp:
            return ApiResponse([])
        recs = (
            FormRecipient.objects.filter(employee=emp)
            .select_related("form")
            .order_by("status", "-form__created_at")
        )
        return ApiResponse(MyFormSerializer(recs, many=True).data)


class ModuleViewSet(viewsets.ReadOnlyModelViewSet):
    """Catálogo global de módulos + banco de preguntas."""

    queryset = Module.objects.prefetch_related("questions").all()
    serializer_class = ModuleSerializer
    permission_classes = [IsHRStaff]
    pagination_class = None


class FormViewSet(TenantScopedViewSet):
    queryset = (
        Form.objects.select_related("created_by", "company")
        .prefetch_related("questions", "modules", "responses", "recipients__employee__department")
        .all()
    )
    search_fields = ["name", "description"]
    filterset_fields = ["status", "ai_generated"]

    def get_permissions(self):
        if self.action in {"create", "update", "partial_update", "destroy", "tracking"}:
            return [IsHRStaff()]
        return [permissions.IsAuthenticated()]

    def get_queryset(self):
        qs = super().get_queryset().annotate(
            response_count=Count("responses", distinct=True),
            recipient_count=Count("recipients", distinct=True),
            completed_count=Count(
                "recipients",
                filter=Q(recipients__status=FormRecipient.Status.COMPLETED),
                distinct=True,
            ),
            pending_count=Count(
                "recipients",
                filter=Q(recipients__status=FormRecipient.Status.PENDING),
                distinct=True,
            ),
        )

        user = self.request.user
        if getattr(user, "role", None) not in {"hr_admin", "super_admin"}:
            employee = getattr(user, "employee", None)
            if not employee:
                return qs.none()
            qs = qs.filter(recipients__employee=employee).distinct()
        return qs

    def get_serializer_class(self):
        if self.action == "create":
            return FormCreateSerializer
        return FormSerializer

    def perform_create(self, serializer):
        company = self.request.user.company
        if not company:
            raise PermissionDenied("No se pudo determinar la empresa del usuario.")
        serializer.save(company=company, created_by=self.request.user)

    @action(detail=True, methods=["get"])
    def tracking(self, request, pk=None):
        form = self.get_object()
        recipients = form.recipients.select_related("employee__department").all()
        now = timezone.now()
        pending = recipients.filter(status=FormRecipient.Status.PENDING)
        completed = recipients.filter(status=FormRecipient.Status.COMPLETED)
        overdue = pending.none()
        if form.deadline and form.deadline < now:
            overdue = pending
        late = completed.none()
        if form.deadline:
            late = completed.filter(completed_at__gt=form.deadline)

        return ApiResponse(
            {
                "form": FormSerializer(form, context=self.get_serializer_context()).data,
                "summary": {
                    "recipient_count": recipients.count(),
                    "completed_count": completed.count(),
                    "pending_count": pending.count(),
                    "overdue_count": overdue.count(),
                    "late_count": late.count(),
                },
                "recipients": FormRecipientSerializer(recipients, many=True, context=self.get_serializer_context()).data,
            }
        )


class ResponseViewSet(TenantScopedViewSet):
    queryset = Response.objects.select_related("form", "employee", "company").prefetch_related(
        "scores__module", "details__form_question"
    )
    filterset_fields = ["form"]

    def get_permissions(self):
        if self.action == "create":
            return [permissions.IsAuthenticated()]
        return [IsHRStaff()]

    def get_serializer_class(self):
        if self.action == "create":
            return ResponseCreateSerializer
        return ResponseSerializer


class AlertViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = AlertSerializer
    queryset = Alert.objects.all()
    permission_classes = [IsHRStaff]
    pagination_class = None

    def get_queryset(self):
        user = self.request.user
        if getattr(user, "role", None) == "super_admin":
            return self.queryset
        return self.queryset.filter(company=user.company_id)


class PredictionsView(APIView):
    """Predicciones del modelo + metadatos."""

    permission_classes = [IsHRStaff]

    def get(self, request):
        company = request.user.company
        qs = (
            Prediction.objects.filter(company=company)
            .select_related("employee__department")
            .order_by("-score")
        )
        meta = {}
        meta_path = Path(settings.BASE_DIR) / "ml" / "meta.json"
        if meta_path.exists():
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
        return ApiResponse({"meta": meta, "results": PredictionSerializer(qs, many=True).data})


class VideoGenerateView(APIView):
    """Sube un video, lo transcribe y genera preguntas mixtas."""

    parser_classes = (MultiPartParser, FormParser)
    permission_classes = [IsHRStaff]

    def post(self, request):
        video_file = request.FILES.get("video")
        if not video_file:
            return ApiResponse({"error": "No se subió ningún video"}, status=400)

        try:
            num_questions = int(request.data.get("num_questions", 5))
        except (TypeError, ValueError):
            num_questions = 5
        num_questions = max(1, min(num_questions, 20))

        raw_types = request.data.get("question_types", "scale")
        question_types = [q.strip() for q in str(raw_types).split(",") if q.strip()]
        if not question_types:
            question_types = ["scale"]

        skills = request.data.get("skills", "Clima laboral, cultura y liderazgo")

        try:
            questions = process_video_and_generate_form(
                video_file, num_questions, question_types, skills
            )
            return ApiResponse({"questions": questions})
        except Exception as exc:
            return ApiResponse({"error": str(exc)}, status=500)
