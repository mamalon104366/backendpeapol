import { useEffect } from 'react';
import Lenis from 'lenis';

let lenis = null;

// Scroll suave global (como el desplazamiento del prototipo del video).
export function useLenis() {
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    lenis = new Lenis({ duration: 1.15, smoothWheel: true });
    let raf = 0;
    const loop = (t) => {
      lenis.raf(t);
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => {
      cancelAnimationFrame(raf);
      lenis.destroy();
      lenis = null;
    };
  }, []);
}

export function scrollToId(id, offset = 0) {
  const el = document.getElementById(id);
  if (!el) return;
  if (lenis) lenis.scrollTo(el, { offset, duration: 1.4 });
  else window.scrollTo({ top: el.getBoundingClientRect().top + window.scrollY + offset, behavior: 'smooth' });
}

export function stopScroll(stop) {
  if (!lenis) return;
  stop ? lenis.stop() : lenis.start();
}
