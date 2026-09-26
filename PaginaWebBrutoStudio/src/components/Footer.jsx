import { useEffect, useState } from 'react';
import Logo, { LogoMark } from './Logo.jsx';
import Reveal from './Reveal.jsx';
import Particles from './Particles.jsx';
import { STUDIO } from '../config.js';
import { scrollToId } from '../hooks/useLenis.js';
import './Footer.css';

const NAV = [
  ['nosotros', 'Nosotros'],
  ['servicios', 'Servicios'],
  ['proyectos', 'Proyectos'],
  ['agendar', 'Agendar'],
];

const SOCIAL_ICONS = {
  instagram: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <circle cx="12" cy="12" r="4.2" />
      <circle cx="17.4" cy="6.6" r="1" fill="currentColor" stroke="none" />
    </>
  ),
  tiktok: <path d="M14 3v11.5a3.5 3.5 0 11-3.5-3.5M14 3c.5 2.6 2.3 4.3 5 4.6" />,
  youtube: (
    <>
      <rect x="2.5" y="5.5" width="19" height="13" rx="4" />
      <path d="M10 9.2v5.6l4.8-2.8z" fill="currentColor" />
    </>
  ),
  behance: <path d="M3 7h5a2.5 2.5 0 010 5H3zm0 5h5.5a2.75 2.75 0 010 5.5H3zM14.5 14h7a3.5 3.5 0 10-1 2.5M15 6.5h5" />,
  linkedin: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="4" />
      <path d="M8 10.5V16M8 7.6v.1M11.5 16v-5.5m0 2.5c0-1.6 1-2.6 2.4-2.6s2.1 1 2.1 2.6V16" />
    </>
  ),
};
const SOCIAL_LABEL = { instagram: 'Instagram', tiktok: 'TikTok', youtube: 'YouTube', behance: 'Behance', linkedin: 'LinkedIn' };

function Banner() {
  return (
    <div className="fbanner" aria-hidden="true">
      <svg className="fbanner__rope" viewBox="0 0 1000 120" preserveAspectRatio="none">
        <path d="M-10 20 C 250 110, 750 110, 1010 20" />
        <path d="M-10 20 C 250 110, 750 110, 1010 20" className="twist" />
      </svg>
      <div className="fbanner__flag">
        <svg viewBox="0 0 400 300" preserveAspectRatio="none">
          <defs>
            <linearGradient id="flag-shade" x1="0" x2="1">
              <stop offset="0" stopColor="#1a1a1a" />
              <stop offset="0.25" stopColor="#3a3a3a" />
              <stop offset="0.5" stopColor="#262626" />
              <stop offset="0.75" stopColor="#3c3c3c" />
              <stop offset="1" stopColor="#161616" />
            </linearGradient>
          </defs>
          <path d="M0 0 H400 V250 C 360 280, 320 240, 280 270 C 240 300, 200 250, 160 280 C 120 305, 80 255, 40 285 C 20 298, 8 290, 0 280 Z" fill="url(#flag-shade)" />
          <path d="M0 0 H400 V14 H0 Z" fill="#0d0d0d" />
        </svg>
        <div className="fbanner__logo">
          <LogoMark size="100%" />
          <span className="display">BRUTO</span>
          <small>STUDIO</small>
        </div>
      </div>
    </div>
  );
}

export default function Footer() {
  const [legal, setLegal] = useState(null);

  useEffect(() => {
    const open = () => {
      const h = window.location.hash.replace('#', '');
      if (h === 'privacidad' || h === 'terminos') setLegal(h);
    };
    open();
    window.addEventListener('hashchange', open);
    return () => window.removeEventListener('hashchange', open);
  }, []);

  const openLegal = (which) => (e) => {
    e.preventDefault();
    setLegal(which);
    setTimeout(() => scrollToId('legal'), 50);
  };

  return (
    <footer id="contacto" className="footer section">
      <div className="footer__bg" aria-hidden="true">
        <div className="footer__windows">
          <span />
          <span />
        </div>
        <div className="footer__particles">
          <Particles density={0.00004} speed={0.6} />
        </div>
      </div>

      <nav className="footer__nav" aria-label="Pie de página">
        {NAV.slice(0, 2).map(([id, l]) => (
          <a key={id} href={`#${id}`} onClick={(e) => { e.preventDefault(); scrollToId(id); }}>{l}</a>
        ))}
        <a href="#inicio" onClick={(e) => { e.preventDefault(); scrollToId('inicio'); }} aria-label="Volver al inicio">
          <Logo size="sm" />
        </a>
        {NAV.slice(2).map(([id, l]) => (
          <a key={id} href={`#${id}`} onClick={(e) => { e.preventDefault(); scrollToId(id); }}>{l}</a>
        ))}
      </nav>

      <Banner />

      <div className="container footer__grid">
        <Reveal className="footer__brand">
          <Logo size="lg" />
          <ul className="footer__social">
            {Object.entries(STUDIO.socials).map(([k, url]) => (
              <li key={k}>
                <a href={url} target="_blank" rel="noopener noreferrer" aria-label={SOCIAL_LABEL[k]}>
                  <svg viewBox="0 0 24 24">{SOCIAL_ICONS[k]}</svg>
                </a>
              </li>
            ))}
          </ul>
        </Reveal>

        <Reveal className="footer__col" delay={0.1}>
          <h3>Llámanos:</h3>
          <a href={`tel:${STUDIO.phone.replace(/[^\d+]/g, '')}`}>{STUDIO.phone}</a>
          <h3>Dirección:</h3>
          <address>
            {STUDIO.address.map((l) => (
              <span key={l}>{l}</span>
            ))}
          </address>
        </Reveal>

        <Reveal className="footer__col" delay={0.2}>
          <h3>Para consultas generales, alianzas o solo para saludar:</h3>
          <a href={`mailto:${STUDIO.emailGeneral}`}>{STUDIO.emailGeneral}</a>
          <h3>Para soporte técnico o dudas de un proyecto en curso:</h3>
          <a href={`mailto:${STUDIO.emailSupport}`}>{STUDIO.emailSupport}</a>
        </Reveal>
      </div>

      <div className="container footer__bottom">
        <p>
          © {STUDIO.year} {STUDIO.name} <span aria-hidden="true">|</span> Todos los derechos reservados
        </p>
        <p>
          <a href="#terminos" onClick={openLegal('terminos')}>Términos y condiciones</a> <span aria-hidden="true">|</span>{' '}
          <a href="#privacidad" onClick={openLegal('privacidad')}>Privacidad</a>
        </p>
      </div>

      <div id="legal" className="container footer__legal">
        {legal && (
          <div className="footer__legal-box">
            <button className="footer__legal-close" onClick={() => setLegal(null)} aria-label="Cerrar">×</button>
            {legal === 'privacidad' ? (
              <>
                <h3 className="display">Aviso de privacidad</h3>
                <p>
                  {STUDIO.name} utiliza los datos que compartes en el formulario (nombre, correo, teléfono y detalles del
                  proyecto) únicamente para organizar la reunión solicitada, preparar una propuesta y darte seguimiento.
                  No vendemos ni compartimos tu información con terceros. Puedes pedir que eliminemos tus datos escribiendo a{' '}
                  {STUDIO.emailGeneral}.
                </p>
                <p className="muted">Texto de ejemplo: revísalo y adáptalo a la legislación de tu país.</p>
              </>
            ) : (
              <>
                <h3 className="display">Términos y condiciones</h3>
                <p>
                  La información de este sitio es de carácter informativo. Las propuestas, alcances, tiempos y costos de cada
                  proyecto se definen por escrito después de la primera reunión. Los trabajos mostrados pertenecen a sus
                  respectivos clientes y se publican con fines de portafolio.
                </p>
                <p className="muted">Texto de ejemplo: revísalo y adáptalo a tu negocio.</p>
              </>
            )}
          </div>
        )}
      </div>
    </footer>
  );
}
