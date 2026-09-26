// ─────────────────────────────────────────────────────────────
//  CONFIGURACIÓN GENERAL DE BRUTO STUDIO
//  Cambia aquí los datos de contacto, redes y el envío del formulario.
// ─────────────────────────────────────────────────────────────

export const STUDIO = {
  name: 'Bruto Studio',
  tagline: 'Crea más allá de lo ordinario',
  phone: '+1 (555) 123-4567', // ← tu teléfono
  whatsapp: '15551234567', // ← número de WhatsApp SIN "+" ni espacios (código de país + número)
  emailGeneral: 'hola@brutostudio.com', // ← correo general
  emailSupport: 'soporte@brutostudio.com', // ← correo de soporte
  address: ['Bruto Studio', 'Calle Creativa 123', 'Tu Ciudad, CP 00000', 'Tu País'],
  year: new Date().getFullYear(),
  socials: {
    instagram: 'https://instagram.com/',
    tiktok: 'https://tiktok.com/',
    youtube: 'https://youtube.com/',
    behance: 'https://behance.net/',
    linkedin: 'https://linkedin.com/',
  },
};

// ─── Formulario de citas ─────────────────────────────────────
// Si pones una URL aquí (Formspree, Make, Zapier, tu propio backend…),
// el formulario enviará un POST con JSON a esa dirección.
// Si lo dejas vacío, al terminar el formulario el cliente envía la
// solicitud por WhatsApp o correo (con todo el resumen ya escrito) y
// puede añadir la cita a su calendario (.ics / Google Calendar).
export const BOOKING_ENDPOINT = ''; // ej: 'https://formspree.io/f/xxxxxxx'

export const BOOKING = {
  defaultCountryCode: '+52',
  // Rangos de presupuesto que verá el cliente (edítalos a tu moneda)
  budgets: [
    'Menos de $500 USD',
    '$500 – $1,500 USD',
    '$1,500 – $5,000 USD',
    '$5,000 – $15,000 USD',
    'Más de $15,000 USD',
    'Aún no lo sé',
  ],
  // Días laborables: 0 = domingo, 1 = lunes … 6 = sábado
  workDays: [1, 2, 3, 4, 5, 6],
  // Horario por día (24 h). El sábado tiene horario reducido.
  hours: {
    default: { start: '09:00', end: '18:00' },
    6: { start: '10:00', end: '14:00' },
  },
  // Pausa de comida (no se ofrecen citas en este rango)
  breakTime: { start: '13:00', end: '14:00' },
  slotMinutes: 30,
  // Horas mínimas de anticipación para agendar
  minNoticeHours: 12,
  // Hasta cuántos días en el futuro se puede agendar
  maxDaysAhead: 60,
  // Horarios ya ocupados, formato 'AAAA-MM-DD HH:MM' (opcional)
  blockedSlots: [],
};
