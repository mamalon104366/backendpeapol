from rest_framework.routers import DefaultRouter

from .views import CompanyViewSet, DepartmentViewSet, EmployeeViewSet

router = DefaultRouter()
router.register("companies", CompanyViewSet, basename="company")
router.register("departments", DepartmentViewSet, basename="department")
router.register("employees", EmployeeViewSet, basename="employee")

urlpatterns = router.urls
