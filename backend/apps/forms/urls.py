from django.urls import path
from rest_framework.routers import DefaultRouter

from .views import (
    AlertViewSet,
    FormViewSet,
    MeProfileView,
    ModuleViewSet,
    MyFormsView,
    PredictionsView,
    ResponseViewSet,
    VideoGenerateView,
)

router = DefaultRouter()
router.register("modules", ModuleViewSet, basename="module")
router.register("forms", FormViewSet, basename="form")
router.register("responses", ResponseViewSet, basename="response")
router.register("alerts", AlertViewSet, basename="alert")

urlpatterns = [
    path("predictions/", PredictionsView.as_view(), name="predictions"),
    path("forms/video-generate/", VideoGenerateView.as_view(), name="video_generate"),
    path("me/profile/", MeProfileView.as_view(), name="me_profile"),
    path("me/forms/", MyFormsView.as_view(), name="me_forms"),
] + router.urls
