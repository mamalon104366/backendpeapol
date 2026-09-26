import Scene from './Scene.jsx';
import FloatCard from './FloatCard.jsx';
import Reveal from './Reveal.jsx';
import BrushButton from './BrushButton.jsx';
import { asset } from '../utils/asset.js';
import { scrollToId } from '../hooks/useLenis.js';
import './About.css';

export default function About() {
  return (
    <>
      {/* ── NOSOTROS ─────────────────────────────── */}
      <section id="nosotros" className="about section">
        <Scene variant="deck" />
        <div className="container about__grid">
          <div className="about__text">
            <Reveal as="h2" className="display h-lg rough">Nosotros</Reveal>
            <Reveal as="p" className="lead" delay={0.1}>
              En Bruto Studio desarrollamos videojuegos, páginas web y software a medida, y damos forma a
              marcas con diseño gráfico, modelado 3D, fotografía y video. Un solo equipo, todas las
              disciplinas, cero límites.
            </Reveal>
            <Reveal delay={0.2}>
              <BrushButton
                href="#contacto"
                onClick={(e) => {
                  e.preventDefault();
                  scrollToId('contacto');
                }}
              >
                Contacto
              </BrushButton>
            </Reveal>
          </div>
          <div className="about__card">
            <FloatCard src={asset('renders/gamepad.webp')} caption={['Damos vida', 'a cada', 'píxel']} tilt={3} />
          </div>
        </div>
      </section>

      {/* ── CREAMOS PRODUCTOS ÚNICOS ─────────────── */}
      <section className="unique section">
        <Scene variant="beams" />
        <div className="container unique__grid">
          <div className="unique__card">
            <FloatCard src={asset('renders/browser.webp')} caption={['Ideas que', 'se vuelven', 'reales']} tilt={-3} />
          </div>
          <div className="unique__text">
            <Reveal as="h2" className="display h-md rough">
              Creamos productos
              <br />
              digitales únicos
            </Reveal>
            <Reveal as="p" className="lead" delay={0.1}>
              Cada proyecto empieza con una idea y termina con algo que se puede jugar, visitar, usar o
              ver. Pensamos como estrategas, diseñamos como artistas y programamos como ingenieros.
            </Reveal>
            <Reveal as="p" className="lead" delay={0.2}>
              Damos vida a los píxeles: juegos que enganchan, webs que venden, sistemas que ahorran horas
              y contenido visual que hace que tu marca no pase desapercibida.
            </Reveal>
          </div>
        </div>
      </section>
    </>
  );
}
