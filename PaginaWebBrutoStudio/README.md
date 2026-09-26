# Bruto Studio — Página web (React)

Sitio one-page de **Bruto Studio** en blanco y negro: desarrollo de videojuegos, páginas web, software,
diseño gráfico, modelado 3D, fotografía y edición de foto y video. Incluye proyectos por categoría y un
formulario de citas de 5 pasos.

Inspirado en el prototipo *"Website design for a mobile game development studio"* (Red Bark Game Studio).
El prompt completo para recrearlo o mejorarlo está en **[SUPER_PROMPT.md](./SUPER_PROMPT.md)**.

---

## 1. Ponerlo en tu escritorio (Windows)

1. Instala **Node.js 20 LTS** (o más reciente) desde <https://nodejs.org>.
2. Descarga esta carpeta (`PaginaWebBrutoStudio`) del repositorio (botón **Code → Download ZIP** en la
   rama donde está, o `git clone`) y cópiala a tu escritorio:
   `C:\Users\TU_USUARIO\Desktop\PaginaWebBrutoStudio`
3. Abre una terminal en esa carpeta y ejecuta:

```bash
npm install      # instala dependencias (solo la primera vez)
npm run dev      # abre la web en http://localhost:5173
```

Otros comandos:

```bash
npm run build    # genera la versión final en /dist
npm run preview  # prueba la versión final localmente
```

---

## 2. Qué incluye

| Sección | Qué hace |
|---|---|
| Menú | Centrado con el logo al medio (como el video). En móvil: menú a pantalla completa. |
| Héroe | Escena con capas en parallax, niebla, partículas, la "tripulación" de objetos 3D en silueta sobre una baranda y animación de entrada. |
| Nosotros / Productos únicos / Misión | Tarjetas con objetos 3D que se salen del marco, fondos ilustrados por sección. |
| Servicios | Las 6 disciplinas desglosadas en sub-servicios, con botón para ver proyectos o agendar sobre ese servicio. |
| Proyectos | Pestañas por categoría + carrusel arrastrable + ventana de detalle de cada proyecto. |
| Agendar cita | Formulario de 5 pasos: servicios → proyecto → reunión (calendario y horarios) → datos → confirmar. Guarda borrador, valida cada paso, genera código, enlace a Google Calendar, archivo .ics y mensaje de WhatsApp/correo. |
| Contacto | Footer con bandera del logo, teléfono, dirección, correos, redes y textos legales. |

---

## 3. Personalizar

Todo lo importante se edita sin tocar el diseño:

| Archivo | Qué cambias |
|---|---|
| `src/config.js` | Teléfono, **WhatsApp**, correos, dirección, redes sociales, horario de citas, días laborables, horarios bloqueados, rangos de presupuesto, lada por defecto y el **endpoint del formulario**. |
| `src/data/services.js` | Servicios y sub-servicios. |
| `src/data/projects.js` | Proyectos. **Son ejemplos (nombres, clientes y cifras inventadas): reemplázalos por los reales antes de publicar.** Agrega `images: ['proyectos/foto1.jpg', 'proyectos/foto2.jpg', 'proyectos/foto3.jpg']` (archivos en `public/proyectos/`) y/o `video: 'https://www.youtube.com/embed/ID'`. |
| `public/renders/` | Objetos 3D (WebP con transparencia). Puedes reemplazarlos con renders propios usando el mismo nombre (ver prompts en SUPER_PROMPT.md, sección 4). |
| `src/components/*.jsx` | Textos de cada sección. |
| `src/styles/global.css` | Colores, tipografías y tamaños globales. |
| `src/components/Footer.jsx` | Textos de *Aviso de privacidad* y *Términos* (son textos de ejemplo: adáptalos a tu país). |

---

## 4. Conectar el formulario de citas

El formulario funciona de dos maneras:

- **Sin endpoint (así viene):** al confirmar, el cliente ve su ticket y envía la solicitud por
  **WhatsApp** o **correo** con un clic (el mensaje ya va redactado con todos los datos). También puede
  añadir la cita a Google Calendar o descargar el `.ics`.
  → Solo cambia `whatsapp` y `emailGeneral` en `src/config.js`.
- **Con endpoint:** pon una URL en `BOOKING_ENDPOINT` (`src/config.js`) y el formulario enviará un `POST`
  con JSON. Ejemplo con **Formspree** (gratis):
  1. Crea un formulario en <https://formspree.io> y copia su URL (`https://formspree.io/f/xxxxxx`).
  2. Pégala en `BOOKING_ENDPOINT`.
  3. Recibirás cada cita por correo. También sirve Make, Zapier, n8n, Google Apps Script o tu propio backend.

Nota: los horarios se muestran en la zona horaria de quien visita la página.

---

## 5. Publicar

- **Vercel** o **Netlify:** importa la carpeta/repositorio, comando de build `npm run build`, carpeta de
  salida `dist`.
- Cualquier hosting estático: sube el contenido de `dist/` (funciona también dentro de una subcarpeta).

---

## 6. Tecnología

React 19 · Vite · framer-motion · lenis (scroll suave) · @fontsource (Lilita One + Baloo 2) · CSS propio.
Sin dependencias de backend.
