import { motion } from 'framer-motion';
import Scene from './Scene.jsx';
import Reveal from './Reveal.jsx';
import BrushButton from './BrushButton.jsx';
import { SERVICES } from '../data/services.js';
import './Services.css';

// Sección con el estilo de "CREW RECRUITMENT" del video: tarjetas claras con
// silueta + brochazo y el botón montado sobre el borde inferior.
export default function Services({ onSeeProjects, onBook }) {
  return (
    <section id="servicios" className="services section">
      <Scene variant="hall" />
      <div className="services__ghosts" aria-hidden="true">
        {SERVICES.slice(0, 3).map((s) => (
          <img key={s.id} src={s.render} alt="" />
        ))}
      </div>
      <div className="container">
        <header className="services__head">
          <Reveal as="h2" className="display h-lg accent rough">
            Nuestros
            <br />
            servicios
          </Reveal>
          <Reveal as="p" className="lead" delay={0.1}>
            Todo lo que tu proyecto necesita, bajo un mismo techo. Elige una disciplina para ver sus
            proyectos o agenda una reunión directamente:
          </Reveal>
        </header>

        <div className="services__grid">
          {SERVICES.map((s, i) => (
            <motion.article
              key={s.id}
              className="scard"
              initial={{ opacity: 0, y: 90, rotate: i % 2 ? 3 : -3 }}
              whileInView={{ opacity: 1, y: 0, rotate: 0 }}
              viewport={{ once: true, amount: 0.2 }}
              transition={{ type: 'spring', stiffness: 110, damping: 16, delay: (i % 3) * 0.08 }}
            >
              <span className="scard__num display">{String(i + 1).padStart(2, '0')}</span>
              <h3 className="scard__title display">{s.title}</h3>
              <p className="scard__pitch">{s.pitch}</p>
              <ul className="scard__list">
                {s.items.map((it) => (
                  <li key={it}>{it}</li>
                ))}
              </ul>
              <button className="scard__book" onClick={() => onBook(s.id)}>
                Agendar reunión sobre esto <span aria-hidden="true">→</span>
              </button>
              <div className="scard__art" aria-hidden="true">
                <svg className="scard__swoosh" viewBox="0 0 240 140">
                  <path d="M18 112 C 50 40, 140 8, 222 46" />
                  <path d="M40 124 C 90 84, 160 70, 214 88" className="thin" />
                </svg>
                <img src={s.render} alt="" loading="lazy" />
              </div>
              <BrushButton as="button" variant="dark" size="sm" className="scard__btn" onClick={() => onSeeProjects(s.id)}>
                Ver proyectos
              </BrushButton>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}
