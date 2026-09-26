// ─────────────────────────────────────────────────────────────
//  PROYECTOS POR CATEGORÍA
//  Son proyectos de EJEMPLO para mostrar el diseño. Reemplázalos por
//  los reales de Bruto Studio.
//
//  Imágenes: si agregas `images: ['/proyectos/mi-foto-1.jpg', ...]`
//  (guardadas en /public/proyectos/), se mostrarán en lugar de las
//  ilustraciones generadas automáticamente.
//  Video: `video: 'https://www.youtube.com/embed/ID'` muestra el botón ▶.
// ─────────────────────────────────────────────────────────────

export const PROJECTS = [
  // ── VIDEOJUEGOS ─────────────────────────────────────────────
  {
    id: 'neon-runner',
    category: 'videojuegos',
    title: ['Neón Runner:', 'Ciudad sin fin'],
    client: 'Proyecto propio',
    year: 2026,
    platform: 'iOS · Android',
    summary:
      'Un endless runner en blanco y negro donde cada salto, deslizamiento y power-up te lleva más lejos por una ciudad que nunca termina.',
    highlights: ['Controles de un solo dedo', 'Niveles generados por procedimiento', 'Tablas de récords en línea'],
    tags: ['Unity', 'Móvil', 'Arte 2D'],
    hasVideo: true,
    seed: 3,
  },
  {
    id: 'ecos-abismo',
    category: 'videojuegos',
    title: ['Ecos del', 'Abismo'],
    client: 'Editorial Faro Games',
    year: 2025,
    platform: 'PC · Nintendo Switch',
    summary:
      'Aventura de plataformas pixel-art con atmósfera melancólica, puzzles de luz y sombra y un jefe final que nadie olvida.',
    highlights: ['12 niveles y 4 jefes', 'Banda sonora original', 'Localizado a 5 idiomas'],
    tags: ['Godot', 'Pixel art', 'Plataformas'],
    hasVideo: true,
    seed: 11,
  },
  {
    id: 'bruto-kart',
    category: 'videojuegos',
    title: ['Bruto Kart:', 'Derrape total'],
    client: 'Marca de bebidas (advergame)',
    year: 2025,
    platform: 'Web · HTML5',
    summary:
      'Advergame de carreras arcade para una campaña de verano: más de 80 mil partidas jugadas en el primer mes.',
    highlights: ['Juega desde el navegador', 'Ranking con premios', 'Integración con redes sociales'],
    tags: ['HTML5', 'Advergame', 'Multijugador'],
    hasVideo: false,
    seed: 27,
  },

  // ── PÁGINAS WEB ─────────────────────────────────────────────
  {
    id: 'cafe-altura',
    category: 'web',
    title: ['Café Altura:', 'Tienda online'],
    client: 'Café Altura',
    year: 2026,
    platform: 'E-commerce',
    summary:
      'Tienda online para una tostadora de café de especialidad: suscripciones mensuales, pagos en línea y una experiencia de compra en 3 clics.',
    highlights: ['+68% de ventas en línea', 'Suscripciones recurrentes', 'Carga en menos de 1 segundo'],
    tags: ['E-commerce', 'SEO', 'Pagos'],
    hasVideo: false,
    seed: 5,
  },
  {
    id: 'monolito',
    category: 'web',
    title: ['Monolito:', 'Arquitectura'],
    client: 'Monolito Arquitectos',
    year: 2025,
    platform: 'Sitio corporativo',
    summary:
      'Sitio corporativo minimalista con galerías a pantalla completa, filtros por tipo de obra y animaciones al hacer scroll.',
    highlights: ['Portafolio con filtros', 'Panel autoadministrable', 'Animaciones suaves'],
    tags: ['Corporativo', 'CMS', 'Animación'],
    hasVideo: true,
    seed: 14,
  },
  {
    id: 'festival-eco',
    category: 'web',
    title: ['Festival Eco:', 'Landing 2026'],
    client: 'Festival Eco',
    year: 2026,
    platform: 'Landing page',
    summary:
      'Landing de alto impacto para un festival de música: line-up interactivo, cuenta regresiva y venta de boletos integrada.',
    highlights: ['12 mil registros', 'Venta de boletos integrada', '100/100 en rendimiento'],
    tags: ['Landing', 'Eventos', 'Conversión'],
    hasVideo: false,
    seed: 22,
  },

  // ── SOFTWARE ────────────────────────────────────────────────
  {
    id: 'stockly',
    category: 'software',
    title: ['Stockly:', 'Inventario vivo'],
    client: 'Distribuidora Norte',
    year: 2026,
    platform: 'Web · Escritorio',
    summary:
      'Sistema de inventario en tiempo real con lectores de código de barras, alertas de stock y reportes automáticos por sucursal.',
    highlights: ['5 sucursales conectadas', 'Reportes automáticos', '-40% en faltantes'],
    tags: ['ERP', 'Dashboard', 'Automatización'],
    hasVideo: false,
    seed: 8,
  },
  {
    id: 'clinica-plus',
    category: 'software',
    title: ['Clínica+:', 'Citas médicas'],
    client: 'Clínica San Rafael',
    year: 2025,
    platform: 'Web · App móvil',
    summary:
      'Plataforma de agenda médica con recordatorios por WhatsApp, expediente digital y cobros en línea.',
    highlights: ['Recordatorios automáticos', 'Expediente digital', '-60% de ausencias'],
    tags: ['SaaS', 'App móvil', 'Integraciones'],
    hasVideo: true,
    seed: 19,
  },
  {
    id: 'ruta-360',
    category: 'software',
    title: ['Ruta 360:', 'Logística'],
    client: 'Transportes Ruta',
    year: 2025,
    platform: 'App móvil · Panel web',
    summary:
      'App para repartidores con rutas optimizadas, evidencia de entrega con foto y un panel de control para la central.',
    highlights: ['Rutas optimizadas', 'Seguimiento en vivo', 'Firma y foto de entrega'],
    tags: ['App móvil', 'Mapas', 'API'],
    hasVideo: false,
    seed: 31,
  },

  // ── DISEÑO GRÁFICO ──────────────────────────────────────────
  {
    id: 'marea',
    category: 'diseno',
    title: ['Marea:', 'Identidad de marca'],
    client: 'Marea Surf Club',
    year: 2026,
    platform: 'Branding',
    summary:
      'Identidad completa para un club de surf: logotipo, tipografía, patrones, uniformes y manual de marca.',
    highlights: ['Logotipo y variantes', 'Manual de marca de 40 páginas', 'Aplicaciones en merch'],
    tags: ['Branding', 'Logotipo', 'Manual'],
    hasVideo: false,
    seed: 4,
  },
  {
    id: 'sonora',
    category: 'diseno',
    title: ['Sonora:', 'Arte musical'],
    client: 'Sonora (banda)',
    year: 2025,
    platform: 'Portada · Merch · Redes',
    summary:
      'Portada de álbum, pósters de gira, merch y un sistema de plantillas para redes sociales.',
    highlights: ['Portada y contraportada', 'Pósters de gira', 'Kit para redes'],
    tags: ['Ilustración', 'Cartelería', 'Redes'],
    hasVideo: false,
    seed: 17,
  },
  {
    id: 'huerto-vivo',
    category: 'diseno',
    title: ['Huerto Vivo:', 'Packaging'],
    client: 'Huerto Vivo',
    year: 2025,
    platform: 'Packaging',
    summary:
      'Línea de empaques para productos orgánicos: etiquetas, cajas y displays para punto de venta.',
    highlights: ['8 productos', 'Empaque sustentable', 'Displays para tienda'],
    tags: ['Packaging', 'Etiquetas', 'Impresión'],
    hasVideo: false,
    seed: 29,
  },

  // ── MODELADO 3D ─────────────────────────────────────────────
  {
    id: 'guardianes',
    category: '3d',
    title: ['Guardianes:', 'Personajes 3D'],
    client: 'Estudio Faro Games',
    year: 2026,
    platform: 'Personajes para juego',
    summary:
      'Serie de personajes estilizados listos para animación: modelado, texturizado, rigging y poses clave.',
    highlights: ['6 personajes', 'Rigging para animación', 'Optimizado para móviles'],
    tags: ['Personajes', 'Rigging', 'Game-ready'],
    hasVideo: true,
    seed: 6,
    render: 'renders/logo_white.webp',
  },
  {
    id: 'loft-21',
    category: '3d',
    title: ['Loft 21:', 'Arquitectura 3D'],
    client: 'Inmobiliaria Horizonte',
    year: 2025,
    platform: 'Visualización arquitectónica',
    summary:
      'Renders fotorrealistas y recorrido virtual de un edificio de lofts antes de su construcción.',
    highlights: ['Recorrido virtual 360°', '20 renders fotorrealistas', 'Preventa agotada'],
    tags: ['Arquitectura', 'Render', '360°'],
    hasVideo: true,
    seed: 13,
    render: 'renders/modeling.webp',
  },
  {
    id: 'aura',
    category: '3d',
    title: ['Aura:', 'Render de producto'],
    client: 'Aura Audio',
    year: 2025,
    platform: 'Render de producto',
    summary:
      'Modelado y render de audífonos para e-commerce y campaña de lanzamiento, con animación 360°.',
    highlights: ['Animación 360°', 'Imágenes para tienda', 'Materiales realistas'],
    tags: ['Producto', 'Animación', 'Publicidad'],
    hasVideo: false,
    seed: 24,
    render: 'renders/camera.webp',
  },

  // ── FOTO Y VIDEO ────────────────────────────────────────────
  {
    id: 'urbano-bn',
    category: 'foto-video',
    title: ['Urbano B/N:', 'Editorial'],
    client: 'Revista Contraste',
    year: 2026,
    platform: 'Fotografía editorial',
    summary:
      'Sesión editorial en blanco y negro por las calles del centro: retrato, moda y arquitectura con retoque profesional.',
    highlights: ['36 fotografías finales', 'Retoque profesional', 'Portada de revista'],
    tags: ['Fotografía', 'Retoque', 'Editorial'],
    hasVideo: false,
    seed: 2,
    photo: 'city',
  },
  {
    id: 'nova',
    category: 'foto-video',
    title: ['Lanzamiento', 'Nova'],
    client: 'Nova Motors',
    year: 2025,
    platform: 'Spot publicitario',
    summary:
      'Spot de 30 segundos y 10 reels para el lanzamiento de un scooter eléctrico: rodaje, edición, color y motion graphics.',
    highlights: ['Spot de 30 s', '10 reels verticales', '+1.2 M de reproducciones'],
    tags: ['Video', 'Motion graphics', 'Reels'],
    hasVideo: true,
    seed: 15,
    photo: 'landscape',
  },
  {
    id: 'sabores',
    category: 'foto-video',
    title: ['Sabores:', 'Foto de producto'],
    client: 'Restaurante Brasa',
    year: 2025,
    platform: 'Fotografía de producto',
    summary:
      'Fotografía gastronómica y de producto para menú, redes y delivery, con edición y retoque de color.',
    highlights: ['60 platillos', 'Menú y delivery', 'Contenido para redes'],
    tags: ['Producto', 'Gastronomía', 'Edición'],
    hasVideo: false,
    seed: 26,
    photo: 'product',
  },
];

export const projectsBy = (category) => PROJECTS.filter((p) => p.category === category);
