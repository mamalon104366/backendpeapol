"""Utilidades compartidas: aislamiento multiempresa (row-level por company_id)."""
from rest_framework import permissions, viewsets


class IsHRStaff(permissions.BasePermission):
    """Permite acceso solo a roles de RR.HH. y super admin."""

    allowed_roles = {"hr_admin", "super_admin"}

    def has_permission(self, request, view):
        user = getattr(request, "user", None)
        return bool(user and user.is_authenticated and getattr(user, "role", None) in self.allowed_roles)


class TenantScopedViewSet(viewsets.ModelViewSet):
    """
    Filtra automáticamente por la empresa del usuario y asigna company al crear.
    El super_admin ve todas las empresas.
    """

    tenant_field = "company"

    def get_queryset(self):
        qs = super().get_queryset()
        user = self.request.user
        if getattr(user, "role", None) == "super_admin":
            return qs
        return qs.filter(**{self.tenant_field: user.company_id})

    def perform_create(self, serializer):
        serializer.save(**{self.tenant_field: self.request.user.company})
