import { LogoMark } from './Logo.jsx';
import './Scene.css';

// Fondos ilustrados por sección (en el video son interiores de un barco
// pirata; aquí son "set de estudio" en blanco y negro).
export default function Scene({ variant = 'deck' }) {
  return (
    <div className={`scene scene--${variant}`} aria-hidden="true">
      <div className="scene__base" />
      {(variant === 'deck' || variant === 'hold') && <div className="scene__planks" />}
      {variant === 'deck' && (
        <>
          <div className="scene__door" />
          <div className="scene__lamp scene__lamp--a" />
          <div className="scene__lamp scene__lamp--b" />
        </>
      )}
      {(variant === 'beams' || variant === 'deck') && (
        <div className="scene__beams">
          <span /><span /><span /><span />
        </div>
      )}
      {variant === 'hold' && (
        <>
          <div className="scene__hang scene__hang--a"><i /></div>
          <div className="scene__hang scene__hang--b"><i /></div>
          <div className="scene__crates">
            <span /><span /><span />
          </div>
        </>
      )}
      {variant === 'mark' && (
        <>
          <div className="scene__flag"><LogoMark size="100%" /></div>
          <div className="scene__poles"><span /><span /><span /></div>
        </>
      )}
      {variant === 'hall' && (
        <div className="scene__windows">
          {Array.from({ length: 5 }).map((_, i) => (
            <span key={i} />
          ))}
        </div>
      )}
      <div className="scene__floor" />
      <div className="scene__vignette" />
    </div>
  );
}
