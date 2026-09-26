import { useEffect, useRef } from 'react';

// Chispas / polvo flotante (en el video son brasas naranjas; aquí, blancas).
export default function Particles({ density = 0.00009, className = '', speed = 1 }) {
  const ref = useRef(null);

  useEffect(() => {
    const canvas = ref.current;
    const ctx = canvas.getContext('2d');
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let w = 0, h = 0, dpr = 1, raf = 0, running = true, parts = [];

    const sprite = document.createElement('canvas');
    sprite.width = sprite.height = 64;
    const s = sprite.getContext('2d');
    const g = s.createRadialGradient(32, 32, 0, 32, 32, 32);
    g.addColorStop(0, 'rgba(255,255,255,1)');
    g.addColorStop(0.18, 'rgba(255,255,255,0.9)');
    g.addColorStop(0.45, 'rgba(255,255,255,0.18)');
    g.addColorStop(1, 'rgba(255,255,255,0)');
    s.fillStyle = g;
    s.fillRect(0, 0, 64, 64);

    const spawn = (anywhere) => ({
      x: Math.random() * w,
      y: anywhere ? Math.random() * h : h + 20,
      r: 2 + Math.random() * 6,
      vy: -(0.15 + Math.random() * 0.55) * speed,
      vx: (Math.random() - 0.5) * 0.25 * speed,
      a: 0.25 + Math.random() * 0.75,
      t: Math.random() * Math.PI * 2,
      f: 0.01 + Math.random() * 0.03,
    });

    const resize = () => {
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      w = canvas.clientWidth;
      h = canvas.clientHeight;
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const n = Math.round(Math.min(140, Math.max(24, w * h * density)));
      parts = Array.from({ length: n }, () => spawn(true));
    };

    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      for (const p of parts) {
        p.t += p.f;
        p.x += p.vx + Math.sin(p.t) * 0.25;
        p.y += p.vy;
        if (p.y < -20 || p.x < -20 || p.x > w + 20) Object.assign(p, spawn(false));
        const flicker = 0.55 + Math.sin(p.t * 3) * 0.45;
        ctx.globalAlpha = p.a * flicker;
        ctx.drawImage(sprite, p.x - p.r * 2, p.y - p.r * 2, p.r * 4, p.r * 4);
      }
      if (running && !reduce) raf = requestAnimationFrame(draw);
    };

    resize();
    draw();
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);
    const io = new IntersectionObserver(([e]) => {
      running = e.isIntersecting;
      cancelAnimationFrame(raf);
      if (running && !reduce) raf = requestAnimationFrame(draw);
    });
    io.observe(canvas);
    return () => {
      cancelAnimationFrame(raf);
      ro.disconnect();
      io.disconnect();
    };
  }, [density, speed]);

  return <canvas ref={ref} className={className} aria-hidden="true" style={{ width: '100%', height: '100%', display: 'block' }} />;
}
