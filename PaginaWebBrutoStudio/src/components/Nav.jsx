import { useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import Logo from './Logo.jsx';
import { scrollToId, stopScroll } from '../hooks/useLenis.js';
import { STUDIO } from '../config.js';
import './Nav.css';

const LEFT = [
  { id: 'nosotros', label: 'Nosotros' },
  { id: 'servicios', label: 'Servicios' },
];
const RIGHT = [
  { id: 'proyectos', label: 'Proyectos' },
  { id: 'agendar', label: 'Agendar cita' },
];
const ALL = [...LEFT, ...RIGHT, { id: 'contacto', label: 'Contacto' }];

export default function Nav() {
  const [scrolled, setScrolled] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let last = window.scrollY;
    const onScroll = () => {
      const y = window.scrollY;
      setScrolled(y > 40);
      setHidden(y > window.innerHeight * 0.9 && y > last + 2);
      if (y < last - 2) setHidden(false);
      last = y;
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    stopScroll(open);
    document.body.style.overflow = open ? 'hidden' : '';
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  const go = (id) => (e) => {
    e.preventDefault();
    setOpen(false);
    setTimeout(() => scrollToId(id), open ? 250 : 0);
  };

  return (
    <>
      <header className={`nav ${scrolled ? 'is-scrolled' : ''} ${hidden && !open ? 'is-hidden' : ''}`}>
        <nav className="nav__inner" aria-label="Principal">
          <ul className="nav__links nav__links--left">
            {LEFT.map((l) => (
              <li key={l.id}>
                <a href={`#${l.id}`} onClick={go(l.id)}>{l.label}</a>
              </li>
            ))}
          </ul>
          <a href="#inicio" className="nav__logo" onClick={go('inicio')} aria-label={`${STUDIO.name} — inicio`}>
            <Logo size="sm" />
          </a>
          <ul className="nav__links nav__links--right">
            {RIGHT.map((l) => (
              <li key={l.id}>
                <a href={`#${l.id}`} onClick={go(l.id)} className={l.id === 'agendar' ? 'nav__cta' : ''}>{l.label}</a>
              </li>
            ))}
          </ul>
          <button
            className={`nav__burger ${open ? 'is-open' : ''}`}
            aria-label={open ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={open}
            aria-controls="mobile-menu"
            onClick={() => setOpen((v) => !v)}
          >
            <span />
            <span />
            <span />
          </button>
        </nav>
      </header>

      <AnimatePresence>
        {open && (
          <motion.div
            id="mobile-menu"
            className="mnav"
            initial={{ clipPath: 'circle(0% at 92% 4%)' }}
            animate={{ clipPath: 'circle(150% at 92% 4%)' }}
            exit={{ clipPath: 'circle(0% at 92% 4%)' }}
            transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          >
            <ul>
              {ALL.map((l, i) => (
                <motion.li
                  key={l.id}
                  initial={{ opacity: 0, y: 30 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.15 + i * 0.06 }}
                >
                  <a href={`#${l.id}`} onClick={go(l.id)} className="display">{l.label}</a>
                </motion.li>
              ))}
            </ul>
            <p className="mnav__foot">{STUDIO.emailGeneral} · {STUDIO.phone}</p>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
