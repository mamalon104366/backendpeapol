# SUPER PROMPT — Sitio web de BRUTO STUDIO

> Documento para recrear (o seguir mejorando) la página web de **Bruto Studio** con cualquier IA de código
> (Claude, ChatGPT, v0, Lovable, Bolt, Cursor…) o para dárselo a un diseñador/desarrollador.
> Está basado en el estudio del video de referencia *"Website design for a mobile game development studio"*.

---

## 1. Qué hay en el video de referencia (análisis)

**Qué es:** un prototipo animado en Figma de una landing page de una página (one-page) para un estudio de
videojuegos móviles ("Red Bark Game Studio"). Estilo ilustrado 3D tipo caricatura (tema pirata), paleta
morado/rosa con acentos rojos. Los últimos ~6 segundos del video son un anuncio de un curso de animación
en Figma y **no** forman parte del diseño.

**Estructura (de arriba a abajo):**

| # | Bloque | Qué tiene |
|---|--------|-----------|
| 1 | **Héroe** | Menú centrado (ABOUT · GAMES · **logo** · JOBS · CONTACT). Escena ilustrada a pantalla completa: paredes de cueva a los lados, barco pirata entre niebla al fondo, brasas flotando, 5 personajes en silueta a contraluz sobre una baranda en primer plano. Título enorme de 3 líneas "PLAY BEYOND ORDINARY" y un botón rojo "OUR GAMES" sobre los personajes. Animación de entrada tipo "cámara" que viaja de los personajes al barco y regresa. |
| 2 | **About** | Título a la izquierda, texto corto y botón "CONTACT". A la derecha, tarjeta lila alta y redondeada con un objeto 3D (botella con pulpo) que **se sale por arriba** de la tarjeta y un lema abajo ("WE BREATHE LIFE INTO PIXELS"). Fondo: interior del barco. |
| 3 | **We create unique gaming products** | Layout invertido: tarjeta con objeto 3D (barco) a la izquierda, título + 2 párrafos a la derecha. Fondo con rayos de luz entrando por una ventana y cayendo en el piso. |
| 4 | **Mission** | Título centrado en color de acento + párrafo. Debajo: bloque de texto a la izquierda, objeto 3D grande flotando al centro (cofre) y otra tarjeta con objeto 3D a la derecha. Fondo: bodega con lámparas colgando. |
| 5 | **Games** | Título a la izquierda + texto. Carrusel horizontal de tarjetas blancas grandes: imagen principal con botón ▶ rojo, dos capturas más, título del juego en 2 líneas, descripción y botón "LEARN MORE". La siguiente tarjeta **asoma** por la derecha. Fondo con el logo gigante desvanecido. |
| 6 | **Crew Recruitment** | Título centrado de 2 líneas + subtítulo. 3 tarjetas lilas altas (puesto, 2 párrafos, silueta de personaje con brochazo rojo detrás y botón "MORE" montado sobre el borde inferior). Fondo con siluetas gigantes desvanecidas. |
| 7 | **Footer / contacto** | Menú repetido arriba, bandera con el logo colgando de una cuerda, ventanas a los lados. Logo grande + redes (Facebook, Instagram, YouTube), teléfono, dirección, correos de contacto y soporte, barra inferior "© 2024 | All rights reserved" y "Terms and Conditions | Privacy". |

**Recursos de estilo clave:** tipografía de cartel gruesa con bordes irregulares, botones con borde de
"brochazo", tarjetas muy redondeadas, objetos 3D que rompen el marco de su tarjeta, fondos ilustrados
distintos por sección, partículas, parallax por capas, apariciones al hacer scroll, desplazamiento suave.

---

## 2. EL SUPER PROMPT (copiar y pegar)

```text
Actúa como un director de arte + desarrollador front-end senior especializado en sitios con animación
(estilo Awwwards). Construye el sitio web completo de BRUTO STUDIO en React, replicando lo más fielmente
posible la estructura, composición y animaciones del prototipo de referencia "Red Bark Game Studio"
(landing one-page de un estudio de videojuegos), pero adaptado a la marca Bruto Studio y ESTRICTAMENTE en
blanco y negro (escala de grises).

═══════════════════════════════════════════════
1) LA MARCA
═══════════════════════════════════════════════
- Nombre: Bruto Studio ("BRUTO" grande + "STUDIO" pequeño y espaciado).
- Qué hace (6 disciplinas): Desarrollo de videojuegos · Páginas web · Software a medida · Diseño gráfico ·
  Modelado 3D · Fotografía y edición de foto y video.
- Personalidad: creativa, directa, "brutalmente creativa", juguetona pero profesional.
- Idioma: español. Tono cercano (tú), frases cortas.
- Logo: monograma "B" muy pesado con una muesca en forma de flecha (›) en el lomo izquierdo, ligeramente
  inclinado (-6°). Encima del texto BRUTO / STUDIO.

═══════════════════════════════════════════════
2) STACK Y ESTRUCTURA
═══════════════════════════════════════════════
- React + Vite (JavaScript), CSS propio por componente (sin Tailwind), framer-motion para animaciones,
  lenis para scroll suave, fuentes con @fontsource (Lilita One + Baloo 2).
- Carpeta del proyecto: PaginaWebBrutoStudio
- Estructura:
  src/config.js            → datos de contacto, WhatsApp, horario de citas, presupuestos, endpoint
  src/data/services.js     → 6 servicios con sus sub-servicios
  src/data/projects.js     → proyectos por categoría (3 por categoría)
  src/components/…         → Nav, Hero, About, Mission, Marquee, Services, Projects, ProjectModal,
                             ProjectArt, booking/(Booking, BookingForm, Calendar), Footer, FloatingCta,
                             Scene (fondos), FloatCard, BrushButton, Particles, Logo, Reveal
  public/renders/          → objetos 3D en PNG/WebP con fondo transparente
- Una sola página con anclas: #inicio #nosotros #servicios #proyectos #agendar #contacto

═══════════════════════════════════════════════
3) SISTEMA DE DISEÑO (solo blanco y negro)
═══════════════════════════════════════════════
Colores:
  --ink #0B0B0B (fondo principal)   --ink-2 #111111   --ink-3 #1A1A1A   --smoke #262626
  --graphite #3A3A3A   --ash #8D8D8D   --mist #BDBDBD (títulos de acento)   --bone #E4E4E2
  --paper #F5F5F3 (blanco cálido de tarjetas y botones)   --white #FFFFFF
  (Donde el video usa ROJO → aquí BLANCO sobre fondo oscuro o NEGRO sobre tarjeta clara.
   Donde usa LILA → gris muy claro #E4E4E2–#F1F1EF.)
Tipografía:
  - Display: "Lilita One", MAYÚSCULAS, interlineado 0.9–0.95, con filtro SVG feTurbulence +
    feDisplacementMap (escala ~3) para bordes irregulares tipo pincel.
  - Texto: "Baloo 2" pesos 600–800.
  - Tamaños: H1 héroe clamp(3rem, 8.4vw, 8rem); H2 clamp(2.4rem, 5.6vw, 4.6rem); cuerpo 17px.
Texturas y detalles:
  - Grano de película animado sobre todo el sitio (SVG noise, opacidad 0.07).
  - Radios grandes: tarjetas 20–30px. Sombras profundas: 0 30px 60px -20px rgba(0,0,0,.75).
  - Botón "brochazo": SVG con forma irregular (polígono con esquinas desiguales) + capa de sombra gris
    desplazada; texto en display. Variante clara (blanco/negro) y oscura (negro/blanco).
    Hover: rotate(-2.5deg) scale(1.06).
  - Objetos 3D: renders monocromáticos brillantes (negro piano + blanco cerámica) con luz de contorno:
    mando de videojuego, ventana de navegador con cursor, símbolos </> con engrane, pluma/herramienta
    pluma con curva bézier, icosaedro con malla de alambre y gizmo XYZ, cámara réflex, claqueta de cine
    y la "B" del logo extruida en 3D (versión negra y blanca).

═══════════════════════════════════════════════
4) SECCIONES (en este orden, con este contenido)
═══════════════════════════════════════════════
0. MENÚ (fijo): centrado: NOSOTROS · SERVICIOS · [LOGO] · PROYECTOS · (AGENDAR CITA en píldora con borde).
   Transparente sobre el héroe; al bajar: fondo negro translúcido con blur. Se oculta al bajar y aparece al
   subir. En móvil: logo a la izquierda + hamburguesa → menú a pantalla completa blanco con enlaces
   gigantes (Nosotros, Servicios, Proyectos, Agendar cita, Contacto) y animación de círculo que se expande.

1. HÉROE (100svh) — réplica del héroe del video en B/N:
   Capas de atrás hacia adelante:
   a) Cielo: degradado radial con núcleo gris claro al centro y bordes negros.
   b) "Monumento": la B 3D blanca enorme, semitransparente, detrás del título (equivale al barco en la niebla).
   c) 3 bancos de niebla blanca desenfocada que se desplazan lentamente de lado a lado.
   d) Estalactitas negras arriba y paredes de roca irregulares a izquierda y derecha (SVG).
   e) Partículas blancas flotando hacia arriba con parpadeo (canvas) = las brasas del video.
   f) Título centrado de 3 líneas: "CREA MÁS / ALLÁ DE LO / ORDINARIO".
   g) "Tripulación" en primer plano: los 7 objetos 3D (pluma, navegador, mando, B negra al centro —la más
      grande—, cámara, código, modelado) en SILUETA a contraluz (brightness .3 + borde luminoso),
      flotando suavemente, detrás de una baranda de balaustres negra que cruza toda la pantalla.
   h) Botón brochazo "NUESTROS PROYECTOS" encima de la tripulación, al centro.
   Animación de entrada: el fondo arranca con zoom 1.35 y blur y se acomoda (2.2s); las rocas entran desde
   los lados; las líneas del título suben con rotación y escalonadas; la baranda y la tripulación suben
   desde abajo (escalonado desde el centro); el botón aparece con un "pop" elástico.
   Parallax al hacer scroll: fondo baja más lento y hace zoom, rocas se abren hacia afuera, el título sube
   y se desvanece. Indicador de scroll (ratón) abajo.

2. NOSOTROS (fondo: pared de tablones, puerta y dos lamparitas que parpadean):
   Izquierda: "NOSOTROS" + "En Bruto Studio desarrollamos videojuegos, páginas web y software a medida, y
   damos forma a marcas con diseño gráfico, modelado 3D, fotografía y video. Un solo equipo, todas las
   disciplinas, cero límites." + botón "CONTACTO".
   Derecha: TARJETA FLOTANTE clara (proporción 0.56) con ventana oscura arriba y el mando 3D saliéndose por
   arriba; lema abajo: "DAMOS VIDA / A CADA / PÍXEL". La tarjeta rota levemente con el scroll y el objeto
   tiene parallax propio.

3. CREAMOS PRODUCTOS DIGITALES ÚNICOS (fondo: haces de luz diagonales sobre el piso):
   Izquierda tarjeta flotante con el navegador 3D: "IDEAS QUE / SE VUELVEN / REALES".
   Derecha título + 2 párrafos (idea → algo que se juega, visita, usa o ve; "damos vida a los píxeles…").

4. MISIÓN (fondo: bodega con lámparas colgantes que se balancean y cajas):
   Centrado: "MISIÓN" (color --mist) + "Convertir ideas en experiencias que se juegan, se usan y se
   recuerdan: con diseño brutalmente honesto, tecnología sólida y una pizca de locura creativa."
   Debajo 3 columnas: texto "SEIS DISCIPLINAS, UN SOLO ESTUDIO" + cifras (6 disciplinas · 1 solo equipo ·
   100% a medida) | B blanca 3D gigante flotando con sombra en el piso | tarjeta con cámara
   "BRUTALMENTE / CREATIVOS".

5. CINTA / MARQUEE: franja blanca inclinada -2° con texto negro infinito: VIDEOJUEGOS · PÁGINAS WEB ·
   SOFTWARE · DISEÑO GRÁFICO · MODELADO 3D · FOTOGRAFÍA · EDICIÓN DE FOTO · EDICIÓN DE VIDEO (con mini
   renders entre palabras). Se pausa al pasar el mouse.

6. NUESTROS SERVICIOS (equivale a "Crew Recruitment"; fondo: salón con ventanas en arco y siluetas
   gigantes desvanecidas de los objetos):
   Centrado: "NUESTROS / SERVICIOS" + "Todo lo que tu proyecto necesita, bajo un mismo techo…".
   6 tarjetas claras (3×2) con: número (01–06), título, frase, LISTA COMPLETA de sub-servicios,
   enlace "Agendar reunión sobre esto →" (abre el formulario con ese servicio ya marcado), zona de arte con
   el objeto 3D en SILUETA negra sobre un BROCHAZO gris (al hacer hover la silueta se "enciende" y se ve
   el render real) y botón brochazo negro "VER PROYECTOS" montado sobre el borde inferior (cambia la
   pestaña de Proyectos a esa categoría y hace scroll).
   En móvil: carrusel horizontal con scroll-snap.
   DESGLOSE:
   - Desarrollo de videojuegos: Juegos móviles (iOS y Android) · Juegos para PC y consola · Juegos web /
     HTML5 · Advergames y gamificación · Prototipos y game design · Arte y animación para juegos ·
     Unity · Unreal · Godot
   - Páginas web: Landing pages · Sitios corporativos · Tiendas online (e-commerce) · Portafolios y blogs ·
     Web apps · SEO y velocidad · Hosting y mantenimiento
   - Software a medida: Software a medida · Apps móviles · Sistemas de gestión (ERP / CRM) · Dashboards y
     reportes · APIs e integraciones · Automatización de procesos · Soluciones con IA
   - Diseño gráfico: Logotipo e identidad de marca · Manual de marca · Contenido para redes sociales ·
     Packaging y etiquetas · Ilustración · Diseño UI / UX · Editorial y cartelería
   - Modelado 3D: Personajes y criaturas · Props y assets para juegos · Render de producto ·
     Visualización arquitectónica · Animación 3D · Modelos para impresión 3D · AR / VR
   - Fotografía y edición de foto y video: Fotografía de producto · Retrato y fotografía corporativa ·
     Cobertura de eventos · Retoque y edición fotográfica · Edición de video · Reels y contenido para
     redes · Motion graphics y spots

7. PROYECTOS (equivale a "Games"; fondo con la B gigante desvanecida a la izquierda y postes):
   Izquierda "PROYECTOS" + texto. Pestañas tipo píldora con mini-render por categoría (la activa en blanco,
   con animación de "píldora" que se desliza entre pestañas: layoutId).
   Carrusel con arrastre (drag) + flechas + puntos + contador "01 / 03": tarjetas blancas enormes; la
   siguiente asoma a la derecha con opacidad .55 y escala .96.
   Anatomía de cada tarjeta (igual al video): fila de medios [imagen principal 16:10 con botón ▶ blanco si
   es video | captura 2 | captura 3] y abajo [TÍTULO en 2 líneas + cliente · año | descripción + etiquetas
   + botón brochazo negro "VER PROYECTO"].
   "VER PROYECTO" abre una ventana modal (portal en body): imagen grande o video embebido, galería,
   categoría, título, resumen, fichas (Cliente · Año · Formato), "Lo más destacado" (3 puntos), etiquetas
   y botón "QUIERO ALGO ASÍ" (lleva al formulario con la categoría preseleccionada). Cierra con Esc, clic
   afuera o botón ×; bloquea el scroll de fondo.
   3 proyectos de ejemplo por categoría (18 en total). Mientras no haya fotos reales, generar portadas
   ilustradas por código en B/N según la categoría: pixel-art para videojuegos, mockups de navegador/móvil
   para web, dashboards y gráficas para software, carteles tipográficos/tarjetas/paleta de grises para
   diseño, renders 3D sobre piso con cuadrícula/cubo de alambre/esferas de arcilla para 3D, y "fotos"
   (paisaje, ciudad, producto con foco, retrato a contraluz, barras de cine + REC para video) con grano.
   Cada proyecto admite `images: [...]` y `video: 'https://youtube.com/embed/…'` para reemplazarlas.

8. AGENDA TU REUNIÓN — FORMULARIO DE CITA MUY ELABORADO:
   Contenedor: gran tarjeta clara con columna lateral negra "¿CÓMO FUNCIONA?" (5 pasos numerados:
   Cuéntanos qué necesitas · Elige día y hora · Te confirmamos · Primera reunión (sin compromiso) ·
   Propuesta) + contacto directo (WhatsApp, correo) y una claqueta 3D flotando en la esquina.
   Formulario por pasos con barra de progreso y pasos clicables (solo hacia atrás o si los anteriores
   son válidos), transición deslizante entre pasos y guardado automático de borrador en localStorage:
   PASO 1 · SERVICIOS: 6 tarjetas seleccionables (render + nombre + ✓), selección múltiple.
     Por cada servicio elegido aparece un grupo de chips con sus sub-servicios (opcional).
     Validación: al menos 1 servicio.
   PASO 2 · PROYECTO: Tipo (Proyecto nuevo · Rediseño o mejora · Mantenimiento o soporte · Asesoría) *,
     Nombre del proyecto/marca, "Cuéntanos tu idea" * (textarea, mínimo 20 caracteres, contador /1200),
     Objetivos (chips múltiples: Vender más, Lanzar un producto, Mejorar mi imagen, Automatizar procesos,
     Entretener / crear comunidad, Crecer en redes, Otro), Presupuesto * (chips únicos configurables:
     Menos de $500 · $500–1,500 · $1,500–5,000 · $5,000–15,000 · Más de $15,000 · Aún no lo sé),
     Plazo * (Lo antes posible · 1–3 meses · 3–6 meses · Sin prisa), Referencias/enlaces a archivos.
   PASO 3 · REUNIÓN: Modalidad * (tarjetas con icono: Videollamada / Presencial / Llamada telefónica);
     si es videollamada, plataforma (Google Meet, Zoom, Microsoft Teams); Duración (30 min charla inicial /
     60 min sesión a fondo); CALENDARIO propio (lunes primero, días pasados, domingos y fuera de rango
     deshabilitados y tachados, navegación de meses limitada a 60 días) + rejilla de HORARIOS (cada 30
     min, lun–vie 9:00–18:00, sáb 10:00–14:00, sin horario de comida 13–14 h, mínimo 12 h de
     anticipación, horarios bloqueados configurables). Muestra la zona horaria detectada.
   PASO 4 · TUS DATOS: Nombre completo *, Empresa/marca, Correo * (validado), Teléfono/WhatsApp * con
     selector de lada (MX +52 por defecto, países de LATAM, EE. UU., España), Cargo, Ciudad y país,
     ¿Cómo nos conociste? (select), ¿Cómo prefieres que te confirmemos? (WhatsApp/Correo/Llamada),
     casilla de novedades y casilla obligatoria de aviso de privacidad. Campo trampa oculto anti-spam.
   PASO 5 · CONFIRMAR: resumen en 4 bloques (Servicios, Proyecto, Reunión, Contacto) cada uno con
     "Editar" que regresa a su paso. Botón "CONFIRMAR CITA".
   ENVÍO: si config.BOOKING_ENDPOINT tiene URL (Formspree, Make, Zapier, backend propio) → POST JSON con
     todo + código de solicitud + fecha ISO + zona horaria + resumen en texto. Si está vacío → la pantalla
     final pide enviar la solicitud por WhatsApp o correo con el mensaje ya redactado.
   PANTALLA DE ÉXITO: check dibujándose (pathLength), "¡Cita solicitada!" / "¡Casi listo!", ticket con
     código BRT-XXXXX, fecha, hora, modalidad y servicios; botones: Enviar por WhatsApp (wa.me con
     texto), Enviar por correo (mailto), Añadir a Google Calendar (URL TEMPLATE), Descargar .ics
     (con recordatorio 30 min antes) y "Agendar otra cita".
   Errores: mensajes en línea con icono "!", bordes punteados, foco al primer error; mensaje de error de
   red con alternativas.
   Accesibilidad: inputs reales (radio/checkbox) estilizados, labels, aria-invalid, aria-describedby,
   foco visible.

9. FOOTER / CONTACTO (igual al del video): menú repetido arriba con el logo al centro; cuerda de lado a lado
   y BANDERA con el logo ondeando (animación suave de skew/rotate); dos ventanas en perspectiva a los lados;
   partículas. Abajo: logo grande + redes (Instagram, TikTok, YouTube, Behance, LinkedIn) | "Llámanos:" +
   teléfono, "Dirección:" | "Para consultas generales, alianzas o solo para saludar:" + correo, "Para soporte
   técnico o dudas de un proyecto en curso:" + correo de soporte. Barra: "© {año} Bruto Studio | Todos los
   derechos reservados" y "Términos y condiciones | Privacidad" (abren textos legales plegables).

10. EXTRA: botón flotante "AGENDAR CITA" (píldora blanca con icono de calendario) que aparece después del
    héroe y se oculta cuando el formulario está en pantalla o al llegar al final.

═══════════════════════════════════════════════
5) ANIMACIÓN (framer-motion + lenis)
═══════════════════════════════════════════════
- Scroll suave global (lenis, duration 1.15). Botones del menú hacen scroll animado a cada sección.
- Reveal genérico: opacity 0→1, y 50→0, blur 8px→0, 0.9s, ease [0.22, 1, 0.36, 1], una sola vez.
- Tarjetas flotantes: entran con spring (scale .7→1, y 80→0), rotan ±4° con el scroll, objeto con
  parallax propio (18%→-14%), hover scale 1.04.
- Objetos 3D: animación "bob" infinita (sube/baja 10–14px con leve rotación), desfasada entre objetos.
- Servicios: tarjetas entran con spring y rotación alternada (±3°), escalonadas.
- Proyectos: cambio de categoría con AnimatePresence (sale a la izquierda, entra desde la derecha);
  carrusel con spring (stiffness 140, damping 22); arrastre con umbral 80px o velocidad 500.
- Formulario: transición horizontal entre pasos según dirección; barra de progreso con spring; chips y
  tarjetas con transición de color/escala.
- Respetar prefers-reduced-motion (desactiva animaciones y lenis).

═══════════════════════════════════════════════
6) RESPONSIVE, ACCESIBILIDAD, SEO, RENDIMIENTO
═══════════════════════════════════════════════
- Breakpoints: 960px (tablet) y 620–760px (móvil). Todo en una columna en móvil; héroe con 5 objetos;
  servicios en carrusel horizontal; tarjetas de proyecto con imagen principal arriba y 2 miniaturas abajo.
- HTML semántico (header/nav/main/section/footer), lang="es", textos alternativos, contraste AA,
  navegación con teclado, foco visible, Esc cierra modales.
- <title> y meta description con las 6 disciplinas, Open Graph, favicon SVG con la B.
- Imágenes WebP con transparencia, carga diferida, partículas pausadas fuera de pantalla.

═══════════════════════════════════════════════
7) ENTREGABLES Y CRITERIOS DE ACEPTACIÓN
═══════════════════════════════════════════════
- Proyecto que corre con: npm install && npm run dev; compila con npm run build sin errores ni warnings.
- Todo el contenido editable desde src/config.js y src/data/*.js.
- La página se ve y se anima como el prototipo de referencia, en blanco y negro, en escritorio y móvil.
- El formulario completo funciona de punta a punta (validaciones, calendario, resumen, envío,
  WhatsApp/correo, Google Calendar, .ics) y guarda el borrador.
- README en español con instrucciones de instalación, personalización, conexión del formulario y
  publicación (Vercel/Netlify).
```

---

## 3. Prompt corto (versión rápida)

```text
Crea en React + Vite una landing one-page para "Bruto Studio" (videojuegos, páginas web, software,
diseño gráfico, modelado 3D, fotografía y edición de foto/video) SOLO en blanco y negro, copiando la
estructura y animaciones del prototipo "Red Bark Game Studio": menú centrado con logo al medio; héroe
ilustrado con capas en parallax (rocas a los lados, niebla, B gigante al fondo, partículas, objetos 3D de
cada servicio en silueta sobre una baranda, título "CREA MÁS ALLÁ DE LO ORDINARIO" y botón con borde de
brochazo); secciones Nosotros / Creamos productos únicos / Misión con tarjetas claras y objetos 3D que se
salen de la tarjeta; servicios desglosados en 6 tarjetas estilo "Crew Recruitment"; proyectos por categoría
en carrusel de tarjetas blancas grandes con modal de detalle; formulario de cita en 5 pasos (servicios →
proyecto → reunión con calendario y horarios → datos → confirmar) con envío a endpoint o WhatsApp, Google
Calendar y .ics; footer con bandera del logo ondeando. Fuentes Lilita One + Baloo 2, framer-motion, lenis,
grano de película, totalmente responsive y accesible.
```

---

## 4. Prompts para generar tus propios renders 3D (Midjourney / DALL·E / Firefly / etc.)

Usa siempre: *"3D render, glossy black piano plastic and white ceramic, monochrome, black and white only,
studio lighting with strong rim light, soft shadows, clay toy style, isolated on transparent / plain
background, 3/4 view, high detail, octane render"* y agrega el objeto:

- **Videojuegos:** `a chunky game controller with white d-pad and buttons`
- **Páginas web:** `a floating browser window with UI blocks and a big mouse cursor`
- **Software:** `the code symbol </> made of white rounded bars with a black gear behind`
- **Diseño gráfico:** `a vector pen tool nib drawing a bezier curve with handles and anchor points`
- **Modelado 3D:** `a white low-poly icosahedron inside a black wireframe sphere with a XYZ gizmo`
- **Fotografía:** `a retro DSLR camera with a big lens`
- **Video:** `an open film clapperboard with black and white stripes`
- **Marca:** `a heavy extruded letter B logo with an arrow notch on its left side`

Guarda los resultados en `public/renders/` con el mismo nombre de archivo para reemplazarlos.
