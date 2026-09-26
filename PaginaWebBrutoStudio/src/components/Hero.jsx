import { useRef } from 'react';
import { motion, useScroll, useTransform } from 'framer-motion';
import BrushButton from './BrushButton.jsx';
import Particles from './Particles.jsx';
import { asset } from '../utils/asset.js';
import { scrollToId } from '../hooks/useLenis.js';
import './Hero.css';

const EASE = [0.22, 1, 0.36, 1];

// "Tripulación" en primer plano: los objetos 3D de cada servicio a contraluz,
// igual que los piratas en silueta del video.
const CREW = [
  { src: 'renders/pen.webp', alt: 'Diseño gráfico', left: 7, w: 12, b: 21, rot: -6, z: 0, hideSm: true },
  { src: 'renders/browser.webp', alt: 'Páginas web', left: 21, w: 14.5, b: 19, rot: -7, z: 1 },
  { src: 'renders/gamepad.webp', alt: 'Videojuegos', left: 35.5, w: 15.5, b: 18, rot: -9, z: 2 },
  { src: 'renders/logo_black.webp', alt: 'Bruto Studio', left: 50, w: 13.5, b: 15, rot: 0, z: 3 },
  { src: 'renders/camera.webp', alt: 'Fotografía y video', left: 64.5, w: 13.5, b: 17, rot: 7, z: 2 },
  { src: 'renders/code.webp', alt: 'Software', left: 79, w: 14, b: 19, rot: 6, z: 1 },
  { src: 'renders/modeling.webp', alt: 'Modelado 3D', left: 93, w: 12.5, b: 21, rot: 5, z: 0, hideSm: true },
];

const TITLE = ['Crea más', 'allá de lo', 'ordinario'];

function Cliff({ side }) {
  const flip = side === 'right' ? 'scale(-1,1) translate(-300,0)' : undefined;
  return (
    <svg viewBox="0 0 300 1000" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <linearGradient id={`cliff-${side}`} x1="0" x2="1">
          <stop offset="0" stopColor="#030303" />
          <stop offset="0.75" stopColor="#101010" />
          <stop offset="1" stopColor="#1d1d1d" />
        </linearGradient>
      </defs>
      <g transform={flip}>
        <path
          d="M0 0 H275 C262 50 296 90 280 140 C266 185 244 200 256 250 C268 300 230 320 232 370 C234 420 262 450 240 500 C220 550 196 560 204 620 C212 680 178 700 180 760 C182 820 150 850 158 910 L150 1000 H0 Z"
          fill="#151515"
          opacity="0.95"
        />
        <path
          d="M0 0 H230 C215 40 250 70 236 110 C224 150 200 160 214 205 C228 250 190 270 186 318 C182 360 214 380 196 430 C178 480 150 490 160 548 C170 600 136 620 138 668 C140 716 116 740 124 800 C132 850 96 880 104 940 L96 1000 H0 Z"
          fill={`url(#cliff-${side})`}
        />
        <path
          d="M230 0 C215 40 250 70 236 110 C224 150 200 160 214 205 C228 250 190 270 186 318 C182 360 214 380 196 430 C178 480 150 490 160 548 C170 600 136 620 138 668 C140 716 116 740 124 800 C132 850 96 880 104 940 L96 1000"
          fill="none"
          stroke="rgba(255,255,255,0.14)"
          strokeWidth="2.5"
        />
        <path d="M200 160 L150 250 L170 330 M170 520 L120 600 L130 690 M110 800 L70 880" stroke="rgba(255,255,255,0.05)" strokeWidth="2" fill="none" />
        <path d="M120 0 C 130 120, 100 190, 118 300 S 104 420, 112 470" stroke="#050505" strokeWidth="3" fill="none" />
        <path d="M160 0 C 170 90, 150 150, 164 230" stroke="#050505" strokeWidth="2" fill="none" />
      </g>
    </svg>
  );
}

function Stalactites() {
  return (
    <svg viewBox="0 0 1000 160" preserveAspectRatio="none" aria-hidden="true">
      <path
        d="M0 0 H1000 V40 L968 70 L950 46 L925 118 L900 58 L862 84 L840 50 L800 66 L770 40 L735 96 L712 44 L680 58 L650 34 L610 52 L590 30 L400 30 L380 54 L350 40 L318 80 L296 48 L262 64 L238 42 L205 104 L182 52 L150 76 L120 44 L88 132 L66 56 L40 74 L18 44 L0 60 Z"
        fill="#070707"
      />
    </svg>
  );
}

function Railing() {
  const posts = Array.from({ length: 34 });
  return (
    <svg className="hero__railing" viewBox="0 0 1700 190" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <linearGradient id="rail-top" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#4a4a4a" />
          <stop offset="0.18" stopColor="#141414" />
          <stop offset="1" stopColor="#080808" />
        </linearGradient>
        <linearGradient id="post" x1="0" x2="1">
          <stop offset="0" stopColor="#050505" />
          <stop offset="0.5" stopColor="#1f1f1f" />
          <stop offset="1" stopColor="#050505" />
        </linearGradient>
      </defs>
      {posts.map((_, i) => {
        const x = 25 + i * 50;
        return (
          <path
            key={i}
            d={`M${x - 9} 58 H${x + 9} C${x + 5} 72 ${x + 13} 88 ${x + 12} 104 C${x + 11} 118 ${x + 5} 128 ${x + 7} 140 H${x - 7} C${x - 5} 128 ${x - 11} 118 ${x - 12} 104 C${x - 13} 88 ${x - 5} 72 ${x - 9} 58 Z`}
            fill="url(#post)"
          />
        );
      })}
      <rect x="0" y="34" width="1700" height="28" rx="6" fill="url(#rail-top)" />
      <rect x="0" y="34" width="1700" height="3" fill="rgba(255,255,255,0.18)" />
      <rect x="0" y="136" width="1700" height="54" fill="#080808" />
      <rect x="0" y="136" width="1700" height="2" fill="rgba(255,255,255,0.08)" />
    </svg>
  );
}

export default function Hero() {
  const ref = useRef(null);
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start start', 'end start'] });
  const skyScale = useTransform(scrollYProgress, [0, 1], [1, 1.18]);
  const farY = useTransform(scrollYProgress, [0, 1], ['0%', '30%']);
  const cliffL = useTransform(scrollYProgress, [0, 1], ['0vw', '-14vw']);
  const cliffR = useTransform(scrollYProgress, [0, 1], ['0vw', '14vw']);
  const titleY = useTransform(scrollYProgress, [0, 1], ['0%', '-60%']);
  const titleO = useTransform(scrollYProgress, [0, 0.55], [1, 0]);
  const crewY = useTransform(scrollYProgress, [0, 1], ['0%', '-8%']);

  return (
    <section id="inicio" className="hero section" ref={ref}>
      <motion.div className="hero__far" style={{ scale: skyScale, y: farY }}>
        <motion.div
          className="hero__far-inner"
          initial={{ scale: 1.35, filter: 'blur(10px)' }}
          animate={{ scale: 1, filter: 'blur(0px)' }}
          transition={{ duration: 2.2, ease: EASE }}
        >
          <div className="hero__sky" />
          <img className="hero__monument" src={asset('renders/logo_white.webp')} alt="" />
          <div className="hero__fog hero__fog--a" />
          <div className="hero__fog hero__fog--b" />
          <div className="hero__fog hero__fog--c" />
        </motion.div>
      </motion.div>

      <motion.div
        className="hero__stalactites"
        initial={{ y: '-100%' }}
        animate={{ y: '0%' }}
        transition={{ duration: 1.6, ease: EASE, delay: 0.1 }}
      >
        <Stalactites />
      </motion.div>

      <motion.div className="hero__cliff hero__cliff--left" style={{ x: cliffL }}>
        <motion.div initial={{ x: '-40%' }} animate={{ x: '0%' }} transition={{ duration: 1.8, ease: EASE }}>
          <Cliff side="left" />
        </motion.div>
      </motion.div>
      <motion.div className="hero__cliff hero__cliff--right" style={{ x: cliffR }}>
        <motion.div initial={{ x: '40%' }} animate={{ x: '0%' }} transition={{ duration: 1.8, ease: EASE }}>
          <Cliff side="right" />
        </motion.div>
      </motion.div>

      <div className="hero__particles">
        <Particles />
      </div>

      <motion.div className="hero__title-wrap" style={{ y: titleY, opacity: titleO }}>
        <h1 className="hero__title display">
          <span className="sr-only">Bruto Studio — </span>
          {TITLE.map((line, i) => (
            <span className="hero__line" key={line}>
              <motion.span
                initial={{ y: '110%', rotate: 4, opacity: 0 }}
                animate={{ y: '0%', rotate: 0, opacity: 1 }}
                transition={{ duration: 1.1, ease: EASE, delay: 0.45 + i * 0.14 }}
              >
                {line}
              </motion.span>
            </span>
          ))}
        </h1>
      </motion.div>

      <motion.div className="hero__crew" style={{ y: crewY }}>
        {CREW.map((c, i) => (
          <motion.div
            key={c.src}
            className={`hero__member ${c.hideSm ? 'hide-sm' : ''}`}
            style={{ left: `${c.left}%`, width: `${c.w}vw`, bottom: `${c.b}%`, zIndex: c.z, x: '-50%' }}
            initial={{ y: '90%', opacity: 0 }}
            animate={{ y: '0%', opacity: 1 }}
            transition={{ duration: 1.3, ease: EASE, delay: 0.9 + Math.abs(3 - i) * 0.09 }}
          >
            <img
              src={asset(c.src)}
              alt={c.alt}
              style={{ '--rot': `${c.rot}deg`, animationDelay: `${i * -0.7}s` }}
            />
          </motion.div>
        ))}
        <motion.div
          className="hero__rail-wrap"
          initial={{ y: '100%' }}
          animate={{ y: '0%' }}
          transition={{ duration: 1.2, ease: EASE, delay: 0.75 }}
        >
          <Railing />
        </motion.div>
      </motion.div>

      <motion.div
        className="hero__cta"
        initial={{ scale: 0, rotate: -12, opacity: 0 }}
        animate={{ scale: 1, rotate: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 220, damping: 14, delay: 1.7 }}
      >
        <BrushButton
          href="#proyectos"
          size="lg"
          onClick={(e) => {
            e.preventDefault();
            scrollToId('proyectos');
          }}
        >
          Nuestros proyectos
        </BrushButton>
      </motion.div>

      <div className="hero__scroll" aria-hidden="true">
        <span />
      </div>
    </section>
  );
}
