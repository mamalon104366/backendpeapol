import { BOOKING, STUDIO } from '../config.js';
import { serviceById } from '../data/services.js';

export const pad = (n) => String(n).padStart(2, '0');
export const toKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const fromKey = (k) => {
  const [y, m, d] = k.split('-').map(Number);
  return new Date(y, m - 1, d);
};
const toMin = (hhmm) => {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
};
const toHHMM = (min) => `${pad(Math.floor(min / 60))}:${pad(min % 60)}`;

export const userTimeZone = () => {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'tu zona horaria';
  } catch {
    return 'tu zona horaria';
  }
};

export function startOfToday() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}

export function slotsFor(key, duration = 30) {
  const date = fromKey(key);
  const day = date.getDay();
  if (!BOOKING.workDays.includes(day)) return [];
  const hours = BOOKING.hours[day] || BOOKING.hours.default;
  const start = toMin(hours.start);
  const end = toMin(hours.end);
  const bStart = BOOKING.breakTime ? toMin(BOOKING.breakTime.start) : -1;
  const bEnd = BOOKING.breakTime ? toMin(BOOKING.breakTime.end) : -1;
  const minTime = Date.now() + BOOKING.minNoticeHours * 3600 * 1000;
  const out = [];
  for (let t = start; t + duration <= end; t += BOOKING.slotMinutes) {
    const hhmm = toHHMM(t);
    const overlapsBreak = t < bEnd && t + duration > bStart;
    const when = new Date(date);
    when.setHours(Math.floor(t / 60), t % 60, 0, 0);
    const blocked = BOOKING.blockedSlots.includes(`${key} ${hhmm}`);
    out.push({ time: hhmm, disabled: overlapsBreak || blocked || when.getTime() < minTime, reason: overlapsBreak ? 'break' : blocked ? 'busy' : 'past' });
  }
  return out.filter((s) => !(s.disabled && s.reason === 'break'));
}

export function isDayAvailable(date, duration = 30) {
  const today = startOfToday();
  const max = new Date(today);
  max.setDate(max.getDate() + BOOKING.maxDaysAhead);
  if (date < today || date > max) return false;
  return slotsFor(toKey(date), duration).some((s) => !s.disabled);
}

const fmtLong = new Intl.DateTimeFormat('es', { weekday: 'long', day: 'numeric', month: 'long' });
export const formatDateLong = (key) => (key ? fmtLong.format(fromKey(key)) : '');

export function startDate(data) {
  if (!data.date || !data.time) return null;
  const d = fromKey(data.date);
  const [h, m] = data.time.split(':').map(Number);
  d.setHours(h, m, 0, 0);
  return d;
}

const icsDate = (d) => d.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');

export const MODES = {
  video: { label: 'Videollamada', hint: 'Google Meet, Zoom o Teams' },
  presencial: { label: 'Presencial', hint: 'En nuestro estudio' },
  llamada: { label: 'Llamada telefónica', hint: 'Te llamamos nosotros' },
};

export function meetingLine(data) {
  const mode = MODES[data.mode]?.label || '';
  const platform = data.mode === 'video' && data.platform ? ` (${data.platform})` : '';
  return `${mode}${platform} · ${formatDateLong(data.date)} · ${data.time} h (${userTimeZone()}) · ${data.duration} min`;
}

export function servicesLine(data) {
  return data.services
    .map((id) => {
      const s = serviceById(id);
      const subs = data.subservices[id] || [];
      return subs.length ? `${s.name} (${subs.join(', ')})` : s.name;
    })
    .join('; ');
}

export function summaryText(data, code) {
  const lines = [
    `Hola ${STUDIO.name}, quiero agendar una reunión.`,
    `Código de solicitud: ${code}`,
    '',
    `Servicios: ${servicesLine(data)}`,
    `Tipo de proyecto: ${data.projectType}${data.projectName ? ` — ${data.projectName}` : ''}`,
    `Idea: ${data.description}`,
    data.goals.length ? `Objetivos: ${data.goals.join(', ')}` : null,
    `Presupuesto: ${data.budget}`,
    `Plazo: ${data.timeline}`,
    data.references ? `Referencias / archivos: ${data.references}` : null,
    '',
    `Reunión: ${meetingLine(data)}`,
    '',
    `Nombre: ${data.name}`,
    data.company ? `Empresa / marca: ${data.company}${data.role ? ` (${data.role})` : ''}` : null,
    `Correo: ${data.email}`,
    `Teléfono / WhatsApp: ${data.countryCode} ${data.phone}`,
    data.city ? `Ciudad / país: ${data.city}` : null,
    data.source ? `Nos conoció por: ${data.source}` : null,
    `Prefiere confirmación por: ${data.contactPref}`,
  ];
  return lines.filter((l) => l !== null).join('\n');
}

export function googleCalendarUrl(data, code) {
  const s = startDate(data);
  if (!s) return '#';
  const e = new Date(s.getTime() + data.duration * 60000);
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: `Reunión con ${STUDIO.name}`,
    dates: `${icsDate(s)}/${icsDate(e)}`,
    details: summaryText(data, code),
    location: data.mode === 'presencial' ? STUDIO.address.join(', ') : MODES[data.mode]?.label || '',
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

const icsEscape = (t) => t.replace(/\\/g, '\\\\').replace(/\n/g, '\\n').replace(/,/g, '\\,').replace(/;/g, '\;');

export function downloadIcs(data, code) {
  const s = startDate(data);
  if (!s) return;
  const e = new Date(s.getTime() + data.duration * 60000);
  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Bruto Studio//Citas//ES',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `UID:${code}@brutostudio`,
    `DTSTAMP:${icsDate(new Date())}`,
    `DTSTART:${icsDate(s)}`,
    `DTEND:${icsDate(e)}`,
    `SUMMARY:${icsEscape(`Reunión con ${STUDIO.name}`)}`,
    `DESCRIPTION:${icsEscape(summaryText(data, code))}`,
    `LOCATION:${icsEscape(data.mode === 'presencial' ? STUDIO.address.join(', ') : MODES[data.mode]?.label || '')}`,
    'BEGIN:VALARM',
    'TRIGGER:-PT30M',
    'ACTION:DISPLAY',
    'DESCRIPTION:Recordatorio',
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n');
  const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `cita-bruto-studio-${code}.ics`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const whatsappUrl = (data, code) => `https://wa.me/${STUDIO.whatsapp}?text=${encodeURIComponent(summaryText(data, code))}`;
export const mailUrl = (data, code) =>
  `mailto:${STUDIO.emailGeneral}?subject=${encodeURIComponent(`Solicitud de reunión ${code} — ${data.name}`)}&body=${encodeURIComponent(summaryText(data, code))}`;

export const makeCode = () => `BRT-${Math.random().toString(36).slice(2, 7).toUpperCase()}`;

export const COUNTRY_CODES = [
  ['+52', 'MX'], ['+1', 'US/CA/PR/DO'], ['+34', 'ES'], ['+54', 'AR'], ['+57', 'CO'], ['+56', 'CL'], ['+51', 'PE'],
  ['+593', 'EC'], ['+58', 'VE'], ['+502', 'GT'], ['+503', 'SV'], ['+504', 'HN'], ['+505', 'NI'], ['+506', 'CR'],
  ['+507', 'PA'], ['+591', 'BO'], ['+595', 'PY'], ['+598', 'UY'], ['+53', 'CU'],
];
