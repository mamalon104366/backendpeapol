import { SERVICES } from '../data/services.js';
import './Marquee.css';

const WORDS = ['Videojuegos', 'Páginas web', 'Software', 'Diseño gráfico', 'Modelado 3D', 'Fotografía', 'Edición de foto', 'Edición de video'];

export default function Marquee() {
  const row = [...WORDS, ...WORDS];
  return (
    <div className="marquee" aria-hidden="true">
      <div className="marquee__track">
        {row.map((w, i) => (
          <span key={i} className="marquee__item display">
            {w}
            <img src={SERVICES[i % SERVICES.length].render} alt="" />
          </span>
        ))}
      </div>
    </div>
  );
}
