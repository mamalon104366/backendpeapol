from django.db import transaction
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from apps.companies.models import Company
from .models import User


class UserSerializer(serializers.ModelSerializer):
    company_name = serializers.CharField(source="company.name", read_only=True, default=None)
    employee_id = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["id", "email", "full_name", "role", "company", "company_name", "employee_id"]

    def get_employee_id(self, obj):
        employee = getattr(obj, "employee", None)
        return employee.id if employee else None


class RegisterSerializer(serializers.Serializer):
    """Registro de empresa: crea la Company + su primer usuario (Admin RH)."""

    company_name = serializers.CharField(max_length=160)
    full_name = serializers.CharField(max_length=160)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=6)
    plan = serializers.CharField(required=False, default="Business")

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("Ya existe una cuenta con este correo.")
        return value.lower()

    @transaction.atomic
    def create(self, data):
        company = Company.objects.create(name=data["company_name"], plan=data.get("plan", "Business"))
        user = User.objects.create_user(
            email=data["email"],
            password=data["password"],
            full_name=data["full_name"],
            role=User.Role.HR_ADMIN,
            company=company,
        )
        return user


class TalentTokenSerializer(TokenObtainPairSerializer):
    """JWT que además devuelve los datos del usuario en el login."""

    def validate(self, attrs):
        data = super().validate(attrs)
        data["user"] = UserSerializer(self.user).data
        return data
