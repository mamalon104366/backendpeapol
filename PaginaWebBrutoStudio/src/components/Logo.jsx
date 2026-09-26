export const LOGO_PATH =
  'M10 6 H60 C80 6 90 17 90 31 C90 40 85.5 46.5 78 49.5 C88.5 52.5 95 61 95 72 C95 86.5 83.5 96 64 96 H10 V62 L21 51 L10 40 Z M34 24 V42 H57 C63.5 42 67 38.3 67 33 C67 27.7 63.5 24 57 24 Z M34 59 V78 H60 C67 78 71 74.3 71 68.5 C71 62.7 67 59 60 59 Z';

export function LogoMark({ size = 40, color = 'currentColor', className = '', title }) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 100 100"
      role={title ? 'img' : undefined}
      aria-hidden={title ? undefined : true}
      aria-label={title}
    >
      <path d={LOGO_PATH} fill={color} fillRule="evenodd" />
    </svg>
  );
}

// Logo apilado (marca arriba, "BRUTO" y "STUDIO" abajo), como en el menú del video.
export default function Logo({ size = 'md', className = '' }) {
  return (
    <span className={`logo logo--${size} ${className}`}>
      <LogoMark className="logo__mark" size="1em" />
      <span className="logo__word">BRUTO</span>
      <span className="logo__sub">STUDIO</span>
    </span>
  );
}
