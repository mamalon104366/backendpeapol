import { motion, useScroll, useTransform } from 'framer-motion';
import { useRef } from 'react';
import Scene from './Scene.jsx';
import FloatCard from './FloatCard.jsx';
import Reveal from './Reveal.jsx';
import { asset } from '../utils/asset.js';
import './Mission.css';

const STATS = [
  { n: '6', l: 'disciplinas' },
  { n: '1', l: 'solo equipo' },
  { n: '100%', l: 'a medida' },
];

export default function Mission() {
  const ref = useRef(null);
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start end', 'end start'] });
  const y = useTransform(scrollYProgress, [0, 1], ['25%', '-25%']);
  const rot = useTransform(scrollYProgress, [0, 1], [-14, 10]);

  return (
    <section className="mission section" ref={ref}>
      <Scene variant="hold" />
      <div className="container">
        <header className="mission__head">
          <Reveal as="h2" className="display h-lg accent rough">Misión</Reveal>
          <Reveal as="p" className="lead" delay={0.1}>
            Convertir ideas en experiencias que se juegan, se usan y se recuerdan: con diseño brutalmente
            honesto, tecnología sólida y una pizca de locura creativa.
          </Reveal>
        </header>

        <div className="mission__grid">
          <div className="mission__text">
            <Reveal as="h3" className="display h-md rough">
              Seis disciplinas,
              <br />
              un solo estudio
            </Reveal>
            <Reveal as="p" delay={0.1}>
              Videojuegos, páginas web, software, diseño gráfico, modelado 3D, fotografía y video trabajando
              juntos. Tu proyecto no pasa de agencia en agencia: todo se crea bajo el mismo techo y con la
              misma visión.
            </Reveal>
            <Reveal className="mission__stats" delay={0.2}>
              {STATS.map((s) => (
                <div key={s.l}>
                  <strong className="display">{s.n}</strong>
                  <span>{s.l}</span>
                </div>
              ))}
            </Reveal>
          </div>

          <div className="mission__center">
            <motion.img
              src={asset('renders/logo_white.webp')}
              alt="Logotipo 3D de Bruto Studio"
              style={{ y, rotate: rot }}
              initial={{ opacity: 0, scale: 0.6 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true, amount: 0.3 }}
              transition={{ type: 'spring', stiffness: 90, damping: 14 }}
            />
            <div className="mission__shadow" />
          </div>

          <div className="mission__card">
            <FloatCard src={asset('renders/camera.webp')} caption={['Brutalmente', 'creativos']} tilt={4} />
          </div>
        </div>
      </div>
    </section>
  );
}
