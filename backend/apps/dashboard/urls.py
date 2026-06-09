from django.urls import path

from .views import ChatView, GenerateQuestionsView, SummaryView

urlpatterns = [
    path("dashboard/summary/", SummaryView.as_view(), name="dashboard-summary"),
    path("chat/", ChatView.as_view(), name="chat"),
    path("generate-questions/", GenerateQuestionsView.as_view(), name="generate-questions"),
]
