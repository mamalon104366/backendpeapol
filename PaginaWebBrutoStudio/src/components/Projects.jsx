import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import Scene from './Scene.jsx';
import Reveal from './Reveal.jsx';
import BrushButton from './BrushButton.jsx';
import ProjectArt from './ProjectArt.jsx';
import ProjectModal from './ProjectModal.jsx';
import { SERVICES, serviceById } from '../data/services.js';
import { projectsBy } from '../data/projects.js';
import './Projects.css';

const GAP = 32;

function Arrow({ dir = 1 }) {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <path d={dir > 0 ? 'M5 12h13M13 6l6 6-6 6' : 'M19 12H6M11 6l-6 6 6 6'} fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ProjectCard({ p, onOpen, active }) {
  return (
    <article className={`pcard ${active ? 'is-active' : ''}`} aria-roledescription="diapositiva">
      <div className="pcard__media">
        <button className="pcard__main" onClick={() => onOpen(p)} aria-label={`Ver ${p.title.join(' ')}`} tabIndex={active ? 0 : -1}>
          <ProjectArt project={p} slot="main" />
          {p.hasVideo && (
            <span className="pcard__play" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M8 5.5v13l11-6.5z" /></svg>
            </span>
          )}
        </button>
        <div className="pcard__thumb"><ProjectArt project={p} slot="a" /></div>
        <div className="pcard__thumb"><ProjectArt project={p} slot="b" /></div>
      </div>
      <div className="pcard__body">
        <div className="pcard__head">
          <h3 className="display">
            {p.title[0]}
            <br />
            {p.title[1]}
          </h3>
          <p className="pcard__meta">
            {p.client} · {p.year}
          </p>
        </div>
        <div className="pcard__info">
          <p>{p.summary}</p>
          <ul className="pcard__tags">
            {p.tags.map((t) => (
              <li key={t}>{t}</li>
            ))}
          </ul>
          <BrushButton as="button" variant="dark" size="sm" onClick={() => onOpen(p)} tabIndex={active ? 0 : -1}>
            Ver proyecto
          </BrushButton>
        </div>
      </div>
    </article>
  );
}

export default function Projects({ category, setCategory, onBook }) {
  const list = projectsBy(category);
  const [index, setIndex] = useState(0);
  const [cardW, setCardW] = useState(900);
  const [open, setOpen] = useState(null);
  const measure = useRef(null);

  useEffect(() => setIndex(0), [category]);

  useLayoutEffect(() => {
    const el = measure.current;
    if (!el) return;
    const ro = new ResizeObserver(() => setCardW(el.getBoundingClientRect().width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const max = list.length - 1;
  const go = (d) => setIndex((i) => Math.max(0, Math.min(max, i + d)));
  const service = serviceById(category);

  return (
    <section id="proyectos" className="projects section">
      <Scene variant="mark" />
      <div className="container projects__head">
        <div>
          <Reveal as="h2" className="display h-lg accent rough">Proyectos</Reveal>
          <Reveal as="p" className="lead" delay={0.1}>
            Una muestra de lo que hacemos en cada disciplina. Elige una categoría y desliza para ver más.
          </Reveal>
        </div>

        <Reveal className="ptabs" delay={0.15} role="tablist" aria-label="Categorías de proyectos">
          {SERVICES.map((s) => (
            <button
              key={s.id}
              role="tab"
              aria-selected={s.id === category}
              className={`ptab ${s.id === category ? 'is-active' : ''}`}
              onClick={() => setCategory(s.id)}
            >
              {s.id === category && <motion.span layoutId="ptab-pill" className="ptab__pill" transition={{ type: 'spring', stiffness: 380, damping: 32 }} />}
              <img src={s.render} alt="" />
              <span>{s.name}</span>
            </button>
          ))}
        </Reveal>
      </div>

      <div className="projects__viewport" aria-live="polite">
        <div className="projects__measure" ref={measure} aria-hidden="true" />
        <AnimatePresence mode="wait">
          <motion.div
            key={category}
            className="projects__track"
            initial={{ opacity: 0, x: 120 }}
            animate={{ opacity: 1, x: -index * (cardW + GAP) }}
            exit={{ opacity: 0, x: -120, transition: { duration: 0.25 } }}
            transition={{ type: 'spring', stiffness: 140, damping: 22 }}
            drag="x"
            dragConstraints={{ left: -max * (cardW + GAP), right: 0 }}
            dragElastic={0.12}
            onDragEnd={(_, info) => {
              if (info.offset.x < -80 || info.velocity.x < -500) go(1);
              else if (info.offset.x > 80 || info.velocity.x > 500) go(-1);
            }}
            style={{ gap: GAP }}
          >
            {list.map((p, i) => (
              <div className="projects__slide" key={p.id} style={{ width: cardW }} onClick={() => i !== index && setIndex(i)}>
                <ProjectCard p={p} onOpen={setOpen} active={i === index} />
              </div>
            ))}
          </motion.div>
        </AnimatePresence>
      </div>

      <div className="container projects__controls">
        <p className="projects__count display">
          <span>{String(index + 1).padStart(2, '0')}</span> / {String(list.length).padStart(2, '0')}
          <em>{service?.name}</em>
        </p>
        <div className="projects__dots">
          {list.map((p, i) => (
            <button key={p.id} className={i === index ? 'is-active' : ''} onClick={() => setIndex(i)} aria-label={`Ir al proyecto ${i + 1}`} />
          ))}
        </div>
        <div className="projects__arrows">
          <button onClick={() => go(-1)} disabled={index === 0} aria-label="Proyecto anterior"><Arrow dir={-1} /></button>
          <button onClick={() => go(1)} disabled={index === max} aria-label="Proyecto siguiente"><Arrow dir={1} /></button>
        </div>
      </div>

      <ProjectModal project={open} onClose={() => setOpen(null)} onBook={(id) => { setOpen(null); onBook(id); }} />
    </section>
  );
}
