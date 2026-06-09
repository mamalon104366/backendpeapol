from rest_framework.response import Response
from rest_framework.views import APIView

from apps.forms.models import AiConversation
from apps.common import IsHRStaff
from .chat import answer_question, generate_questions
from .services import compute_summary


class SummaryView(APIView):
    """Resumen agregado del estado de la empresa (alimenta el dashboard RH)."""

    permission_classes = [IsHRStaff]

    def get(self, request):
        return Response(compute_summary(request.user.company))


class ChatView(APIView):
    """Copiloto IA: responde preguntas en lenguaje natural sobre los datos."""

    permission_classes = [IsHRStaff]

    def post(self, request):
        message = (request.data.get("message") or "").strip()
        if not message:
            return Response({"detail": "Mensaje vacío."}, status=400)
        mode = (request.data.get("mode") or "normal").strip().lower()
        if mode not in ("conciso", "normal", "extenso"):
            mode = "normal"
        company = request.user.company
        answer, source = answer_question(message, company, mode)
        AiConversation.objects.create(company=company, question=message, answer=answer, source=source)
        return Response({"answer": answer, "source": source})


class GenerateQuestionsView(APIView):
    """Genera preguntas para un análisis personalizado (IA o plantilla)."""

    permission_classes = [IsHRStaff]

    def post(self, request):
        topic = (request.data.get("topic") or "").strip()
        if not topic:
            return Response({"detail": "Indica un tema."}, status=400)
        n = int(request.data.get("n") or 4)
        questions, source = generate_questions(topic, max(2, min(n, 6)))
        return Response({"topic": topic, "questions": questions, "source": source})
