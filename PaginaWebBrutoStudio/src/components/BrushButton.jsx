import './BrushButton.css';

// Botón con borde de "brochazo", inspirado en los botones rojos del video,
// adaptado a blanco y negro. variant: 'light' (blanco) | 'dark' (negro).
export default function BrushButton({ as: Tag = 'a', variant = 'light', size = 'md', children, className = '', ...props }) {
  return (
    <Tag className={`bbtn bbtn--${variant} bbtn--${size} ${className}`} {...props}>
      <svg className="bbtn__bg" viewBox="0 0 220 64" preserveAspectRatio="none" aria-hidden="true">
        <path
          className="bbtn__shadow"
          d="M9 14 L48 9 L120 12 L205 7 L214 26 L211 52 L200 62 L118 59 L36 63 L7 58 L10 36 Z"
        />
        <path
          className="bbtn__fill"
          d="M6 8 L44 4 L118 7 L200 2 L210 18 L207 44 L196 55 L114 52 L32 57 L3 51 L6 30 Z"
        />
        <path className="bbtn__streak" d="M18 13 L70 11 M140 10 L190 8" />
      </svg>
      <span className="bbtn__label">{children}</span>
    </Tag>
  );
}
