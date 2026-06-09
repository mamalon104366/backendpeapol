from django.contrib.auth import get_user_model
from rest_framework import serializers

from .models import Company, Department, Employee

User = get_user_model()


class CompanySerializer(serializers.ModelSerializer):
    employee_count = serializers.IntegerField(source="employees.count", read_only=True)

    class Meta:
        model = Company
        fields = ["id", "name", "plan", "is_active", "created_at", "employee_count"]
        read_only_fields = ["created_at"]


class DepartmentSerializer(serializers.ModelSerializer):
    employee_count = serializers.IntegerField(source="employees.count", read_only=True)

    class Meta:
        model = Department
        fields = ["id", "name", "employee_count"]


class EmployeeSerializer(serializers.ModelSerializer):
    department_name = serializers.CharField(source="department.name", read_only=True, default=None)
    # email (write): si se envía, crea una cuenta de acceso (rol empleado)
    email = serializers.EmailField(required=False, allow_blank=True, write_only=True)
    account_email = serializers.EmailField(source="user.email", read_only=True, default=None)
    has_account = serializers.SerializerMethodField()

    class Meta:
        model = Employee
        fields = [
            "id",
            "full_name",
            "role_title",
            "department",
            "department_name",
            "hire_date",
            "email",
            "account_email",
            "has_account",
            "created_at",
        ]
        read_only_fields = ["created_at"]

    def get_has_account(self, obj):
        return obj.user_id is not None

    def validate_email(self, value):
        value = (value or "").strip().lower()
        if value and User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("Ya existe una cuenta con este correo.")
        return value

    def create(self, validated_data):
        email = (validated_data.pop("email", "") or "").strip().lower()
        company = validated_data.get("company")
        employee = super().create(validated_data)
        if email:
            user = User.objects.create_user(
                email=email,
                password="people123",  # contraseña temporal demo
                full_name=employee.full_name,
                role=User.Role.EMPLOYEE,
                company=company,
            )
            employee.user = user
            employee.save(update_fields=["user"])
        return employee

    def update(self, instance, validated_data):
        validated_data.pop("email", None)
        return super().update(instance, validated_data)


class MeProfileSerializer(serializers.ModelSerializer):
    """Perfil propio del empleado. Solo phone/birth_date/about son editables."""

    area = serializers.CharField(source="department.name", read_only=True, default="Sin área")
    account_email = serializers.EmailField(source="user.email", read_only=True, default=None)
    company_name = serializers.CharField(source="company.name", read_only=True, default=None)
    tenure_days = serializers.SerializerMethodField()
    forms_assigned = serializers.SerializerMethodField()
    forms_completed = serializers.SerializerMethodField()
    forms_pending = serializers.SerializerMethodField()

    class Meta:
        model = Employee
        fields = [
            "id", "full_name", "role_title", "area", "company_name", "hire_date", "tenure_days",
            "phone", "birth_date", "about", "account_email",
            "forms_assigned", "forms_completed", "forms_pending",
        ]
        read_only_fields = [
            "id", "full_name", "role_title", "area", "company_name", "hire_date", "account_email",
        ]

    def get_tenure_days(self, obj):
        if not obj.hire_date:
            return None
        from datetime import date
        return (date.today() - obj.hire_date).days

    def get_forms_assigned(self, obj):
        return obj.form_recipients.count()

    def get_forms_completed(self, obj):
        return obj.form_recipients.filter(status="completed").count()

    def get_forms_pending(self, obj):
        return obj.form_recipients.filter(status="pending").count()
