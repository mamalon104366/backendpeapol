import { useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { scrollToId } from '../hooks/useLenis.js';

// Botón flotante "Agendar cita": aparece después de la portada y se oculta
// cuando el formulario ya está en pantalla.
export default function FloatingCta() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    let bookingVisible = false;
    const target = document.getElementById('agendar');
    const io = target
      ? new IntersectionObserver(([e]) => {
          bookingVisible = e.isIntersecting;
          update();
        }, { threshold: 0.05 })
      : null;
    io?.observe(target);
    function update() {
      const nearBottom = window.scrollY + window.innerHeight > document.documentElement.scrollHeight - 220;
      setShow(window.scrollY > window.innerHeight * 0.9 && !bookingVisible && !nearBottom);
    }
    update();
    window.addEventListener('scroll', update, { passive: true });
    return () => {
      window.removeEventListener('scroll', update);
      io?.disconnect();
    };
  }, []);

  return (
    <AnimatePresence>
      {show && (
        <motion.a
          href="#agendar"
          className="fcta"
          initial={{ opacity: 0, y: 40, scale: 0.8 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 40, scale: 0.8 }}
          whileHover={{ rotate: -3, scale: 1.05 }}
          onClick={(e) => {
            e.preventDefault();
            scrollToId('agendar');
          }}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="3" />
            <path d="M3 10h18M8 3v4M16 3v4" />
          </svg>
          Agendar cita
        </motion.a>
      )}
    </AnimatePresence>
  );
}
