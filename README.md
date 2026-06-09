# RRHH Deploy

Esta carpeta tiene el backend listo para subir a Render.

## Qué subir

- Sube `C:\Users\mamalon\Desktop\RRHH-Deploy` como repo o carpeta de trabajo.
- No subas `.venv`, `db.sqlite3` ni `.env`.

## Opción recomendada: Render

### 1. Crear el servicio

- Entra a Render.
- Crea un `Web Service`.
- Conecta el repo o sube el proyecto.
- Usa la raíz del proyecto como carpeta base.

### 2. Build command

```bash
pip install -r backend/requirements.txt
```

### 3. Start command

```bash
gunicorn config.wsgi:application --chdir backend
```

### 4. Variables de entorno

Configura estas variables en Render:

```bash
SECRET_KEY=pon_una_clave_larga_y_nueva
DEBUG=False
ALLOWED_HOSTS=tu-backend.onrender.com
DATABASE_URL=tu_postgres_url
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app
ACCESS_TOKEN_LIFETIME_MIN=120
REFRESH_TOKEN_LIFETIME_DAYS=7
GEMINI_API_KEY=tu_clave_si_la_usas
```

### 5. Migraciones

Cuando el servicio ya exista, ejecuta:

```bash
python backend/manage.py migrate
```

Si Render no te deja correrlo manualmente, usa el shell del servicio.

### 6. Datos demo

Después de desplegar, puedes sembrar la demo con:

```bash
python backend/manage.py seed_catalog
python backend/manage.py seed_roster
```

Credenciales demo:

- `alexander.amaris@prueba.com`
- `people123`

## Frontend

Cuando el backend ya tenga URL pública:

- Cambia `VITE_API_URL` en Vercel a `https://tu-backend.onrender.com/api`

## Nota

El backend local ya fue probado en `127.0.0.1:8000`.
