import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import BrushButton from '../BrushButton.jsx';
import Calendar from './Calendar.jsx';
import { SERVICES, serviceById } from '../../data/services.js';
import { BOOKING, BOOKING_ENDPOINT } from '../../config.js';
import {
  COUNTRY_CODES,
  MODES,
  downloadIcs,
  formatDateLong,
  googleCalendarUrl,
  mailUrl,
  makeCode,
  meetingLine,
  servicesLine,
  slotsFor,
  startDate,
  summaryText,
  userTimeZone,
  whatsappUrl,
} from '../../utils/booking.js';

const STEPS = ['Servicios', 'Proyecto', 'Reunión', 'Tus datos', 'Confirmar'];

const PROJECT_TYPES = [
  { v: 'Proyecto nuevo', d: 'Empezamos desde cero' },
  { v: 'Rediseño o mejora', d: 'Ya existe y quieres llevarlo más lejos' },
  { v: 'Mantenimiento o soporte', d: 'Cuidado continuo de lo que ya tienes' },
  { v: 'Asesoría', d: 'Quieres orientación antes de empezar' },
];
const GOALS = ['Vender más', 'Lanzar un producto', 'Mejorar mi imagen', 'Automatizar procesos', 'Entretener / crear comunidad', 'Crecer en redes', 'Otro'];
const TIMELINES = ['Lo antes posible (menos de 1 mes)', '1 – 3 meses', '3 – 6 meses', 'Sin prisa / flexible'];
const PLATFORMS = ['Google Meet', 'Zoom', 'Microsoft Teams'];
const DURATIONS = [
  { v: 30, l: '30 min', d: 'Charla inicial' },
  { v: 60, l: '60 min', d: 'Sesión a fondo' },
];
const SOURCES = ['Instagram', 'TikTok', 'Facebook', 'YouTube', 'Google', 'Recomendación', 'Ya soy cliente', 'Otro'];
const PREFS = ['WhatsApp', 'Correo', 'Llamada'];

const EMPTY = {
  services: [],
  subservices: {},
  projectType: '',
  projectName: '',
  description: '',
  goals: [],
  budget: '',
  timeline: '',
  references: '',
  mode: '',
  platform: 'Google Meet',
  duration: 30,
  date: '',
  time: '',
  name: '',
  company: '',
  role: '',
  email: '',
  countryCode: BOOKING.defaultCountryCode,
  phone: '',
  city: '',
  source: '',
  contactPref: 'WhatsApp',
  newsletter: false,
  privacy: false,
  website: '', // campo trampa anti-spam (invisible)
};

const DRAFT_KEY = 'bruto-booking-draft-v1';
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (raw) return { ...EMPTY, ...JSON.parse(raw), website: '' };
  } catch {
    /* sin almacenamiento disponible */
  }
  return EMPTY;
}

function validate(step, d) {
  const e = {};
  if (step === 0 && d.services.length === 0) e.services = 'Elige al menos un servicio.';
  if (step === 1) {
    if (!d.projectType) e.projectType = 'Selecciona el tipo de proyecto.';
    if (d.description.trim().length < 20) e.description = 'Cuéntanos un poco más (mínimo 20 caracteres).';
    if (!d.budget) e.budget = 'Elige un rango de presupuesto.';
    if (!d.timeline) e.timeline = 'Elige un plazo.';
  }
  if (step === 2) {
    if (!d.mode) e.mode = 'Elige cómo quieres reunirte.';
    if (!d.date) e.date = 'Elige un día en el calendario.';
    else if (!d.time) e.time = 'Elige una hora disponible.';
  }
  if (step === 3) {
    if (d.name.trim().length < 3) e.name = 'Escribe tu nombre completo.';
    if (!EMAIL_RE.test(d.email.trim())) e.email = 'Escribe un correo válido.';
    if (d.phone.replace(/\D/g, '').length < 7) e.phone = 'Escribe un teléfono válido (mínimo 7 dígitos).';
    if (!d.privacy) e.privacy = 'Necesitamos tu autorización para contactarte.';
  }
  return e;
}

// ── Piezas de UI ───────────────────────────────────────────
function Choice({ type = 'checkbox', name, checked, onChange, className = 'bchip', children, invalid }) {
  return (
    <label className={`${className} ${checked ? 'is-on' : ''}`}>
      <input type={type} name={name} checked={checked} onChange={onChange} className="sr-only" aria-invalid={invalid || undefined} />
      {children}
    </label>
  );
}

function Err({ id, children }) {
  return children ? (
    <p className="berr" id={id} role="alert">
      {children}
    </p>
  ) : null;
}

function Field({ label, required, error, hint, children, full, htmlFor }) {
  return (
    <div className={`bfield ${full ? 'bfield--full' : ''} ${error ? 'has-error' : ''}`}>
      <label className="blabel" htmlFor={htmlFor}>
        {label} {required && <span aria-hidden="true">*</span>}
      </label>
      {children}
      {hint && !error && <p className="bhint">{hint}</p>}
      <Err id={`${htmlFor}-err`}>{error}</Err>
    </div>
  );
}

function Group({ title, hint, error, children, id }) {
  return (
    <fieldset className={`bgroup ${error ? 'has-error' : ''}`} aria-describedby={error ? `${id}-err` : undefined}>
      <legend className="blabel">{title}</legend>
      {hint && <p className="bhint bhint--top">{hint}</p>}
      {children}
      <Err id={`${id}-err`}>{error}</Err>
    </fieldset>
  );
}

const ICONS = {
  video: <path d="M3 7a2 2 0 012-2h9a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2zM16 10l5-3v10l-5-3" />,
  presencial: <path d="M12 21s-7-6.2-7-11.5A7 7 0 0119 9.5C19 14.8 12 21 12 21zm0-8.5a3 3 0 100-6 3 3 0 000 6z" />,
  llamada: <path d="M5 4h4l2 5-2.5 1.5a11 11 0 005 5L15 13l5 2v4a2 2 0 01-2 2A16 16 0 013 6a2 2 0 012-2" />,
};

function SummaryBlock({ title, onEdit, children }) {
  return (
    <div className="bsum">
      <div className="bsum__head">
        <h4 className="display">{title}</h4>
        <button type="button" onClick={onEdit}>
          Editar
        </button>
      </div>
      <div className="bsum__body">{children}</div>
    </div>
  );
}

// ── Formulario ─────────────────────────────────────────────
export default function BookingForm({ preset }) {
  const uid = useId().replace(/[^a-zA-Z0-9]/g, '');
  const [data, setData] = useState(loadDraft);
  const [step, setStep] = useState(0);
  const [dir, setDir] = useState(1);
  const [errors, setErrors] = useState({});
  const [status, setStatus] = useState('idle'); // idle | sending | done | error
  const [code, setCode] = useState('');
  const [saved, setSaved] = useState(false);
  const rootRef = useRef(null);

  // Guardado automático del borrador
  useEffect(() => {
    if (status === 'done') return;
    const t = setTimeout(() => {
      try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify({ ...data, website: '' }));
        setSaved(true);
      } catch {
        /* ignorar */
      }
    }, 500);
    return () => clearTimeout(t);
  }, [data, status]);

  // Servicio preseleccionado desde otras secciones ("Agendar reunión sobre esto")
  useEffect(() => {
    if (!preset?.service) return;
    if (status === 'done') {
      setData({ ...EMPTY, services: [preset.service] });
      setStatus('idle');
      setCode('');
    } else {
      setData((d) => (d.services.includes(preset.service) ? d : { ...d, services: [...d.services, preset.service] }));
    }
    setErrors({});
    setStep(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset]);

  const set = (patch) => {
    setData((d) => ({ ...d, ...patch }));
    setErrors((e) => {
      const n = { ...e };
      Object.keys(patch).forEach((k) => delete n[k]);
      return n;
    });
  };
  const toggleList = (key, v) => set({ [key]: data[key].includes(v) ? data[key].filter((x) => x !== v) : [...data[key], v] });
  const toggleService = (id) => {
    const on = data.services.includes(id);
    const subs = { ...data.subservices };
    if (on) delete subs[id];
    set({ services: on ? data.services.filter((x) => x !== id) : [...data.services, id], subservices: subs });
  };
  const toggleSub = (id, item) => {
    const cur = data.subservices[id] || [];
    set({ subservices: { ...data.subservices, [id]: cur.includes(item) ? cur.filter((x) => x !== item) : [...cur, item] } });
  };

  const scrollIntoForm = () => {
    const el = rootRef.current;
    if (!el) return;
    const top = el.getBoundingClientRect().top;
    if (top < 0) window.scrollTo({ top: window.scrollY + top - 90, behavior: 'smooth' });
  };

  const focusFirstError = () =>
    setTimeout(() => {
      const el = rootRef.current?.querySelector('.has-error input, .has-error textarea, .has-error select, .has-error button');
      el?.focus({ preventScroll: false });
    }, 60);

  const next = () => {
    const e = validate(step, data);
    setErrors(e);
    if (Object.keys(e).length) return focusFirstError();
    setDir(1);
    setStep((s) => Math.min(s + 1, STEPS.length - 1));
    scrollIntoForm();
  };
  const back = () => {
    setDir(-1);
    setErrors({});
    setStep((s) => Math.max(0, s - 1));
    scrollIntoForm();
  };
  const goTo = (i) => {
    if (i === step) return;
    if (i < step) {
      setDir(-1);
      setErrors({});
      setStep(i);
      return;
    }
    for (let s = step; s < i; s++) {
      const e = validate(s, data);
      if (Object.keys(e).length) {
        setStep(s);
        setErrors(e);
        return focusFirstError();
      }
    }
    setDir(1);
    setStep(i);
  };

  const submit = async () => {
    if (data.website) return; // bot detectado
    for (let s = 0; s < 4; s++) {
      const e = validate(s, data);
      if (Object.keys(e).length) {
        setStep(s);
        setErrors(e);
        return focusFirstError();
      }
    }
    setStatus('sending');
    const c = makeCode();
    const { website, ...clean } = data;
    const payload = {
      code: c,
      ...clean,
      services: data.services.map((id) => serviceById(id)?.name),
      start: startDate(data)?.toISOString(),
      timezone: userTimeZone(),
      summary: summaryText(data, c),
      submittedAt: new Date().toISOString(),
    };
    try {
      if (BOOKING_ENDPOINT) {
        const res = await fetch(BOOKING_ENDPOINT, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
      } else {
        await new Promise((r) => setTimeout(r, 900));
      }
      setCode(c);
      setStatus('done');
      scrollIntoForm();
      try {
        localStorage.removeItem(DRAFT_KEY);
      } catch {
        /* ignorar */
      }
    } catch {
      setCode(c);
      setStatus('error');
    }
  };

  const reset = () => {
    setData(EMPTY);
    setStep(0);
    setStatus('idle');
    setErrors({});
    setCode('');
    setSaved(false);
    try {
      localStorage.removeItem(DRAFT_KEY);
    } catch {
      /* ignorar */
    }
  };

  const slots = useMemo(() => (data.date ? slotsFor(data.date, data.duration) : []), [data.date, data.duration]);
  const tz = userTimeZone();
  const firstName = data.name.trim().split(' ')[0];
  const id = (n) => `${uid}-${n}`;

  // ── Pantalla de éxito ──
  if (status === 'done') {
    return (
      <div className="bform" ref={rootRef}>
        <motion.div className="bsuccess" initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <svg className="bsuccess__check" viewBox="0 0 80 80" aria-hidden="true">
            <motion.circle cx="40" cy="40" r="36" fill="none" stroke="currentColor" strokeWidth="5" initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.7 }} />
            <motion.path d="M24 41l11 11 22-24" fill="none" stroke="currentColor" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.5, delay: 0.6 }} />
          </svg>
          <h3 className="display">{BOOKING_ENDPOINT ? '¡Cita solicitada!' : '¡Casi listo!'}</h3>
          <p className="bsuccess__lead">
            {BOOKING_ENDPOINT
              ? `Gracias, ${firstName}. Recibimos tu solicitud y te confirmaremos por ${data.contactPref.toLowerCase()}.`
              : `Gracias, ${firstName}. Envíanos la solicitud con un clic para apartar tu horario (el mensaje ya va escrito):`}
          </p>
          <div className="bsuccess__ticket">
            <span className="bsuccess__code display">{code}</span>
            <p>{meetingLine(data)}</p>
            <p className="muted">{servicesLine(data)}</p>
          </div>
          <div className="bsuccess__actions">
            {!BOOKING_ENDPOINT && (
              <>
                <BrushButton href={whatsappUrl(data, code)} target="_blank" rel="noopener noreferrer" variant="dark">
                  Enviar por WhatsApp
                </BrushButton>
                <a className="bbtn-ghost" href={mailUrl(data, code)}>
                  Enviar por correo
                </a>
              </>
            )}
            <a className="bbtn-ghost" href={googleCalendarUrl(data, code)} target="_blank" rel="noopener noreferrer">
              Añadir a Google Calendar
            </a>
            <button type="button" className="bbtn-ghost" onClick={() => downloadIcs(data, code)}>
              Descargar .ics
            </button>
          </div>
          <button type="button" className="blink" onClick={reset}>
            Agendar otra cita
          </button>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="bform" ref={rootRef}>
      <ol className="bsteps" aria-label="Pasos del formulario">
        {STEPS.map((label, i) => (
          <li key={label} className={i === step ? 'is-current' : i < step ? 'is-done' : ''}>
            <button type="button" onClick={() => goTo(i)} aria-current={i === step ? 'step' : undefined}>
              <span className="bsteps__n">{i < step ? '✓' : i + 1}</span>
              <span className="bsteps__l">{label}</span>
            </button>
          </li>
        ))}
      </ol>
      <div className="bprogress" aria-hidden="true">
        <motion.span animate={{ width: `${(step / (STEPS.length - 1)) * 100}%` }} transition={{ type: 'spring', stiffness: 120, damping: 20 }} />
      </div>

      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          step === STEPS.length - 1 ? submit() : next();
        }}
      >
        <input
          className="bhoney"
          name="website"
          tabIndex={-1}
          autoComplete="off"
          value={data.website}
          onChange={(e) => set({ website: e.target.value })}
          aria-hidden="true"
        />

        <AnimatePresence mode="wait" custom={dir} initial={false}>
          <motion.div
            key={step}
            className="bstep"
            custom={dir}
            initial={{ opacity: 0, x: dir * 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: dir * -50 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            {/* ── PASO 1: SERVICIOS ── */}
            {step === 0 && (
              <>
                <h3 className="bstep__title display">¿Qué necesitas?</h3>
                <p className="bstep__hint">Elige uno o varios servicios. Después marca lo que te interesa de cada uno (opcional).</p>
                <Group title="Servicios" id={id('services')} error={errors.services}>
                  <div className="bservices">
                    {SERVICES.map((s) => (
                      <Choice key={s.id} className="bservice" checked={data.services.includes(s.id)} onChange={() => toggleService(s.id)} invalid={!!errors.services}>
                        <img src={s.render} alt="" />
                        <span className="bservice__name">{s.name}</span>
                        <span className="bservice__check" aria-hidden="true">✓</span>
                      </Choice>
                    ))}
                  </div>
                </Group>
                <AnimatePresence initial={false}>
                  {data.services.map((sid) => {
                    const s = serviceById(sid);
                    return (
                      <motion.div key={sid} className="bsubs" initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}>
                        <Group title={`${s.name}: ¿qué te interesa?`} id={id(`sub-${sid}`)}>
                          <div className="bchips">
                            {s.items.map((it) => (
                              <Choice key={it} checked={(data.subservices[sid] || []).includes(it)} onChange={() => toggleSub(sid, it)}>
                                {it}
                              </Choice>
                            ))}
                          </div>
                        </Group>
                      </motion.div>
                    );
                  })}
                </AnimatePresence>
              </>
            )}

            {/* ── PASO 2: PROYECTO ── */}
            {step === 1 && (
              <>
                <h3 className="bstep__title display">Cuéntanos tu proyecto</h3>
                <p className="bstep__hint">Mientras más sepamos, mejor preparada llegará la reunión.</p>
                <Group title="Tipo de proyecto *" id={id('ptype')} error={errors.projectType}>
                  <div className="btiles">
                    {PROJECT_TYPES.map((t) => (
                      <Choice key={t.v} type="radio" name={id('ptype')} className="btile" checked={data.projectType === t.v} onChange={() => set({ projectType: t.v })} invalid={!!errors.projectType}>
                        <strong>{t.v}</strong>
                        <span>{t.d}</span>
                      </Choice>
                    ))}
                  </div>
                </Group>
                <div className="bgrid">
                  <Field label="Nombre del proyecto o marca" htmlFor={id('pname')} full>
                    <input id={id('pname')} className="binput" value={data.projectName} onChange={(e) => set({ projectName: e.target.value })} placeholder="Ej. Café Altura" autoComplete="organization" />
                  </Field>
                  <Field label="Cuéntanos tu idea" required htmlFor={id('desc')} error={errors.description} full>
                    <textarea
                      id={id('desc')}
                      className="binput"
                      rows={4}
                      maxLength={1200}
                      value={data.description}
                      onChange={(e) => set({ description: e.target.value })}
                      placeholder="¿Qué quieres crear? ¿Para quién es? ¿Qué problema resuelve?"
                      aria-invalid={!!errors.description || undefined}
                      aria-describedby={errors.description ? `${id('desc')}-err` : undefined}
                    />
                    <span className="bcount">{data.description.length} / 1200</span>
                  </Field>
                </div>
                <Group title="Objetivos (opcional)" id={id('goals')}>
                  <div className="bchips">
                    {GOALS.map((g) => (
                      <Choice key={g} checked={data.goals.includes(g)} onChange={() => toggleList('goals', g)}>
                        {g}
                      </Choice>
                    ))}
                  </div>
                </Group>
                <Group title="Presupuesto estimado *" id={id('budget')} error={errors.budget}>
                  <div className="bchips">
                    {BOOKING.budgets.map((b) => (
                      <Choice key={b} type="radio" name={id('budget')} checked={data.budget === b} onChange={() => set({ budget: b })} invalid={!!errors.budget}>
                        {b}
                      </Choice>
                    ))}
                  </div>
                </Group>
                <Group title="¿Para cuándo lo necesitas? *" id={id('timeline')} error={errors.timeline}>
                  <div className="bchips">
                    {TIMELINES.map((t) => (
                      <Choice key={t} type="radio" name={id('timeline')} checked={data.timeline === t} onChange={() => set({ timeline: t })} invalid={!!errors.timeline}>
                        {t}
                      </Choice>
                    ))}
                  </div>
                </Group>
                <div className="bgrid">
                  <Field label="Referencias o archivos (opcional)" htmlFor={id('refs')} hint="Pega enlaces a webs, juegos o marcas que te gusten, o a tus archivos en Drive, Dropbox o WeTransfer." full>
                    <input id={id('refs')} className="binput" value={data.references} onChange={(e) => set({ references: e.target.value })} placeholder="https://…" />
                  </Field>
                </div>
              </>
            )}

            {/* ── PASO 3: REUNIÓN ── */}
            {step === 2 && (
              <>
                <h3 className="bstep__title display">Elige tu reunión</h3>
                <p className="bstep__hint">Selecciona la modalidad, la duración y el horario que mejor te acomode.</p>
                <Group title="Modalidad *" id={id('mode')} error={errors.mode}>
                  <div className="btiles btiles--3">
                    {Object.entries(MODES).map(([k, m]) => (
                      <Choice key={k} type="radio" name={id('mode')} className="btile btile--icon" checked={data.mode === k} onChange={() => set({ mode: k })} invalid={!!errors.mode}>
                        <svg viewBox="0 0 24 24" aria-hidden="true">{ICONS[k]}</svg>
                        <strong>{m.label}</strong>
                        <span>{m.hint}</span>
                      </Choice>
                    ))}
                  </div>
                </Group>
                <AnimatePresence initial={false}>
                  {data.mode === 'video' && (
                    <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}>
                      <Group title="Plataforma" id={id('platform')}>
                        <div className="bchips">
                          {PLATFORMS.map((p) => (
                            <Choice key={p} type="radio" name={id('platform')} checked={data.platform === p} onChange={() => set({ platform: p })}>
                              {p}
                            </Choice>
                          ))}
                        </div>
                      </Group>
                    </motion.div>
                  )}
                </AnimatePresence>
                <Group title="Duración" id={id('dur')}>
                  <div className="bchips">
                    {DURATIONS.map((d) => (
                      <Choice key={d.v} type="radio" name={id('dur')} checked={data.duration === d.v} onChange={() => set({ duration: d.v, time: '' })}>
                        <b>{d.l}</b>&nbsp;· {d.d}
                      </Choice>
                    ))}
                  </div>
                </Group>
                <div className={`bwhen ${errors.date || errors.time ? 'has-error' : ''}`}>
                  <Calendar value={data.date} duration={data.duration} onChange={(k) => set({ date: k, time: '' })} />
                  <div className="bslots">
                    <p className="bslots__title display">{data.date ? formatDateLong(data.date) : 'Elige un día'}</p>
                    {data.date ? (
                      <div className="bslots__grid" role="radiogroup" aria-label="Horarios disponibles">
                        {slots.map((s) => (
                          <button
                            type="button"
                            key={s.time}
                            role="radio"
                            aria-checked={data.time === s.time}
                            disabled={s.disabled}
                            className={`bslot ${data.time === s.time ? 'is-on' : ''}`}
                            onClick={() => set({ time: s.time })}
                          >
                            {s.time}
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="bhint">Los días en gris no están disponibles.</p>
                    )}
                    <p className="bslots__tz">Horario mostrado en: {tz}</p>
                    <Err id={id('when-err')}>{errors.date || errors.time}</Err>
                  </div>
                </div>
              </>
            )}

            {/* ── PASO 4: DATOS ── */}
            {step === 3 && (
              <>
                <h3 className="bstep__title display">¿Cómo te contactamos?</h3>
                <p className="bstep__hint">Solo usaremos tus datos para organizar esta reunión.</p>
                <div className="bgrid">
                  <Field label="Nombre completo" required htmlFor={id('name')} error={errors.name}>
                    <input id={id('name')} className="binput" value={data.name} onChange={(e) => set({ name: e.target.value })} autoComplete="name" aria-invalid={!!errors.name || undefined} />
                  </Field>
                  <Field label="Empresa o marca" htmlFor={id('company')}>
                    <input id={id('company')} className="binput" value={data.company} onChange={(e) => set({ company: e.target.value })} autoComplete="organization" />
                  </Field>
                  <Field label="Correo electrónico" required htmlFor={id('email')} error={errors.email}>
                    <input id={id('email')} type="email" className="binput" value={data.email} onChange={(e) => set({ email: e.target.value })} autoComplete="email" inputMode="email" aria-invalid={!!errors.email || undefined} />
                  </Field>
                  <Field label="Teléfono / WhatsApp" required htmlFor={id('phone')} error={errors.phone}>
                    <div className="bphone">
                      <select className="binput" value={data.countryCode} onChange={(e) => set({ countryCode: e.target.value })} aria-label="Código de país">
                        {COUNTRY_CODES.map(([c, n]) => (
                          <option key={`${c}${n}`} value={c}>
                            {n} {c}
                          </option>
                        ))}
                      </select>
                      <input id={id('phone')} type="tel" className="binput" value={data.phone} onChange={(e) => set({ phone: e.target.value })} autoComplete="tel-national" inputMode="tel" aria-invalid={!!errors.phone || undefined} />
                    </div>
                  </Field>
                  <Field label="Cargo" htmlFor={id('role')}>
                    <input id={id('role')} className="binput" value={data.role} onChange={(e) => set({ role: e.target.value })} autoComplete="organization-title" />
                  </Field>
                  <Field label="Ciudad y país" htmlFor={id('city')}>
                    <input id={id('city')} className="binput" value={data.city} onChange={(e) => set({ city: e.target.value })} autoComplete="address-level2" />
                  </Field>
                  <Field label="¿Cómo nos conociste?" htmlFor={id('source')} full>
                    <select id={id('source')} className="binput" value={data.source} onChange={(e) => set({ source: e.target.value })}>
                      <option value="">Selecciona una opción</option>
                      {SOURCES.map((s) => (
                        <option key={s}>{s}</option>
                      ))}
                    </select>
                  </Field>
                </div>
                <Group title="¿Cómo prefieres que te confirmemos?" id={id('pref')}>
                  <div className="bchips">
                    {PREFS.map((p) => (
                      <Choice key={p} type="radio" name={id('pref')} checked={data.contactPref === p} onChange={() => set({ contactPref: p })}>
                        {p}
                      </Choice>
                    ))}
                  </div>
                </Group>
                <label className="bcheck">
                  <input type="checkbox" checked={data.newsletter} onChange={(e) => set({ newsletter: e.target.checked })} />
                  <span>Quiero recibir novedades y proyectos de Bruto Studio (máximo una vez al mes).</span>
                </label>
                <div className={errors.privacy ? 'has-error' : ''}>
                  <label className="bcheck">
                    <input type="checkbox" checked={data.privacy} onChange={(e) => set({ privacy: e.target.checked })} aria-invalid={!!errors.privacy || undefined} />
                    <span>
                      Acepto el <a href="#privacidad">aviso de privacidad</a> y que Bruto Studio me contacte sobre esta solicitud. *
                    </span>
                  </label>
                  <Err id={id('privacy-err')}>{errors.privacy}</Err>
                </div>
              </>
            )}

            {/* ── PASO 5: CONFIRMAR ── */}
            {step === 4 && (
              <>
                <h3 className="bstep__title display">Revisa y confirma</h3>
                <p className="bstep__hint">Verifica que todo esté bien. Puedes editar cualquier sección.</p>
                <div className="bsummary">
                  <SummaryBlock title="Servicios" onEdit={() => goTo(0)}>
                    <p>{servicesLine(data)}</p>
                  </SummaryBlock>
                  <SummaryBlock title="Proyecto" onEdit={() => goTo(1)}>
                    <p>
                      <b>{data.projectType}</b>
                      {data.projectName && ` — ${data.projectName}`}
                    </p>
                    <p className="bsum__quote">“{data.description}”</p>
                    {data.goals.length > 0 && <p>Objetivos: {data.goals.join(', ')}</p>}
                    <p>
                      Presupuesto: {data.budget} · Plazo: {data.timeline}
                    </p>
                    {data.references && <p>Referencias: {data.references}</p>}
                  </SummaryBlock>
                  <SummaryBlock title="Reunión" onEdit={() => goTo(2)}>
                    <p>{meetingLine(data)}</p>
                  </SummaryBlock>
                  <SummaryBlock title="Contacto" onEdit={() => goTo(3)}>
                    <p>
                      <b>{data.name}</b>
                      {data.company && ` · ${data.company}`}
                      {data.role && ` (${data.role})`}
                    </p>
                    <p>
                      {data.email} · {data.countryCode} {data.phone}
                    </p>
                    <p>Confirmación por: {data.contactPref}</p>
                  </SummaryBlock>
                </div>
              </>
            )}
          </motion.div>
        </AnimatePresence>

        {status === 'error' && (
          <p className="berr berr--box" role="alert">
            No pudimos enviar tu solicitud. Inténtalo de nuevo o envíala por{' '}
            <a href={whatsappUrl(data, code)} target="_blank" rel="noopener noreferrer">
              WhatsApp
            </a>{' '}
            o <a href={mailUrl(data, code)}>correo</a>.
          </p>
        )}

        <div className="bform__nav">
          {step > 0 ? (
            <button type="button" className="blink" onClick={back}>
              ← Atrás
            </button>
          ) : (
            <span className="bform__draft">{saved ? 'Tu avance se guarda automáticamente' : ''}</span>
          )}
          <div className="bform__right">
            <span className="bform__count">
              Paso {step + 1} de {STEPS.length}
            </span>
            <BrushButton as="button" type="submit" variant="dark" disabled={status === 'sending'}>
              {step === STEPS.length - 1 ? (status === 'sending' ? 'Enviando…' : 'Confirmar cita') : 'Siguiente'}
            </BrushButton>
          </div>
        </div>
      </form>
    </div>
  );
}
