from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from apps.common import IsHRStaff, TenantScopedViewSet
from .models import Company, Department, Employee
from .serializers import CompanySerializer, DepartmentSerializer, EmployeeSerializer


class CompanyViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = CompanySerializer
    queryset = Company.objects.all()
    permission_classes = [IsHRStaff]

    def get_queryset(self):
        user = self.request.user
        if getattr(user, "role", None) == "super_admin":
            return self.queryset
        return self.queryset.filter(id=user.company_id)


class DepartmentViewSet(TenantScopedViewSet):
    serializer_class = DepartmentSerializer
    queryset = Department.objects.all().order_by("name")
    pagination_class = None
    permission_classes = [IsHRStaff]


class EmployeeViewSet(TenantScopedViewSet):
    serializer_class = EmployeeSerializer
    queryset = Employee.objects.select_related("department").order_by("full_name")
    filterset_fields = ["department"]
    search_fields = ["full_name", "role_title"]
    permission_classes = [IsHRStaff]

    @action(detail=False, methods=["get"])
    def all(self, request):
        employees = self.get_queryset().select_related("department", "user")
        return Response(self.get_serializer(employees, many=True).data)
