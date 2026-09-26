import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import ProjectArt from './ProjectArt.jsx';
import BrushButton from './BrushButton.jsx';
import { serviceById } from '../data/services.js';
import { stopScroll } from '../hooks/useLenis.js';
import './ProjectModal.css';

export default function ProjectModal({ project, onClose, onBook }) {
  const closeRef = useRef(null);

  useEffect(() => {
    if (!project) return;
    stopScroll(true);
    document.body.style.overflow = 'hidden';
    const prev = document.activeElement;
    closeRef.current?.focus();
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => {
      stopScroll(false);
      document.body.style.overflow = '';
      window.removeEventListener('keydown', onKey);
      prev?.focus?.();
    };
  }, [project, onClose]);

  const s = project && serviceById(project.category);

  return createPortal(
    <AnimatePresence>
      {project && (
        <motion.div
          className="pmodal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="pmodal-title"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            className="pmodal__panel"
            data-lenis-prevent
            initial={{ y: 80, scale: 0.94, rotate: -1.5 }}
            animate={{ y: 0, scale: 1, rotate: 0 }}
            exit={{ y: 60, scale: 0.96, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 170, damping: 20 }}
            onClick={(e) => e.stopPropagation()}
          >
            <button ref={closeRef} className="pmodal__close" onClick={onClose} aria-label="Cerrar">
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="3" strokeLinecap="round" /></svg>
            </button>

            <div className="pmodal__hero">
              {project.video ? (
                <iframe src={project.video} title={project.title.join(' ')} allow="autoplay; encrypted-media; picture-in-picture" allowFullScreen />
              ) : (
                <ProjectArt project={project} slot="main" />
              )}
            </div>
            <div className="pmodal__gallery">
              <ProjectArt project={project} slot="a" />
              <ProjectArt project={project} slot="b" />
            </div>

            <div className="pmodal__content">
              <p className="pmodal__cat">{s?.title}</p>
              <h3 id="pmodal-title" className="display">
                {project.title[0]} {project.title[1]}
              </h3>
              <p className="pmodal__summary">{project.summary}</p>

              <dl className="pmodal__facts">
                <div><dt>Cliente</dt><dd>{project.client}</dd></div>
                <div><dt>Año</dt><dd>{project.year}</dd></div>
                <div><dt>Formato</dt><dd>{project.platform}</dd></div>
              </dl>

              <h4 className="display">Lo más destacado</h4>
              <ul className="pmodal__highlights">
                {project.highlights.map((h) => (
                  <li key={h}>{h}</li>
                ))}
              </ul>

              <ul className="pmodal__tags">
                {project.tags.map((t) => (
                  <li key={t}>{t}</li>
                ))}
              </ul>

              <div className="pmodal__actions">
                <BrushButton as="button" variant="dark" onClick={() => onBook(project.category)}>
                  Quiero algo así
                </BrushButton>
                <button className="pmodal__back" onClick={onClose}>Seguir viendo proyectos</button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body
  );
}
