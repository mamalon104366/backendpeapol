import Scene from '../Scene.jsx';
import Reveal from '../Reveal.jsx';
import BookingForm from './BookingForm.jsx';
import { STUDIO } from '../../config.js';
import { asset } from '../../utils/asset.js';
import './Booking.css';

const HOW = [
  ['Cuéntanos qué necesitas', 'Elige servicios y describe tu idea en 2 minutos.'],
  ['Elige día y hora', 'Videollamada, llamada o en persona en el estudio.'],
  ['Te confirmamos', 'Por WhatsApp, correo o llamada, como prefieras.'],
  ['Primera reunión', 'Escuchamos tu idea y te damos una ruta clara, sin compromiso.'],
  ['Propuesta', 'Recibes alcance, tiempos y cotización del proyecto.'],
];

export default function Booking({ preset }) {
  return (
    <section id="agendar" className="booking section">
      <Scene variant="beams" />
      <div className="container">
        <header className="booking__head">
          <Reveal as="h2" className="display h-lg rough">
            Agenda tu
            <br />
            reunión
          </Reveal>
          <Reveal as="p" className="lead" delay={0.1}>
            Cuéntanos tu idea y elige el horario que te acomode. Llegaremos a la reunión con preguntas,
            ideas y referencias para tu proyecto.
          </Reveal>
        </header>

        <Reveal className="booking__card" y={80} amount={0.1}>
          <aside className="booking__aside">
            <img className="booking__deco" src={asset('renders/clapper.webp')} alt="" />
            <h3 className="display">¿Cómo funciona?</h3>
            <ol className="booking__how">
              {HOW.map(([t, d], i) => (
                <li key={t}>
                  <span className="display">{i + 1}</span>
                  <div>
                    <strong>{t}</strong>
                    <p>{d}</p>
                  </div>
                </li>
              ))}
            </ol>
            <div className="booking__alt">
              <p>¿Prefieres escribirnos directo?</p>
              <a href={`https://wa.me/${STUDIO.whatsapp}`} target="_blank" rel="noopener noreferrer">WhatsApp · {STUDIO.phone}</a>
              <a href={`mailto:${STUDIO.emailGeneral}`}>{STUDIO.emailGeneral}</a>
            </div>
          </aside>
          <BookingForm preset={preset} />
        </Reveal>
      </div>
    </section>
  );
}
