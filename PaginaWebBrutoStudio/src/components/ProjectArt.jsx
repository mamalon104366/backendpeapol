import { useId, useMemo } from 'react';
import { rng } from '../utils/random.js';
import { asset } from '../utils/asset.js';

// ─────────────────────────────────────────────────────────────
//  Ilustraciones generadas por código (blanco y negro) que sirven
//  de portada mientras no haya fotos reales del proyecto.
//  Si el proyecto tiene `images`, se usan esas en su lugar.
// ─────────────────────────────────────────────────────────────

const SIZES = { main: [320, 200], a: [160, 200], b: [160, 200], wide: [320, 180] };
const INK = '#0b0b0b';
const PAPER = '#f5f5f3';

export default function ProjectArt({ project, slot = 'main', className = '' }) {
  const rawId = useId();
  const uid = rawId.replace(/[^a-zA-Z0-9]/g, '');
  const idx = { main: 0, a: 1, b: 2, wide: 0 }[slot] ?? 0;
  const real = project.images?.[idx];
  const [W, H] = SIZES[slot] || SIZES.main;

  const content = useMemo(() => {
    if (real) return null;
    const r = rng(project.seed * 97 + idx * 13 + 1);
    const ctx = { W, H, r, uid, slot, project };
    switch (project.category) {
      case 'videojuegos':
        return slot === 'main' || slot === 'wide' ? pixelScene(ctx) : slot === 'a' ? pixelTower(ctx) : pixelHero(ctx);
      case 'web':
        return slot === 'main' || slot === 'wide' ? webDesktop(ctx) : slot === 'a' ? webPhone(ctx) : webComponents(ctx);
      case 'software':
        return slot === 'main' || slot === 'wide' ? dashboard(ctx) : slot === 'a' ? appList(ctx) : donut(ctx);
      case 'diseno':
        return slot === 'main' || slot === 'wide' ? poster(ctx) : slot === 'a' ? cards(ctx) : swatches(ctx);
      case '3d':
        return slot === 'main' || slot === 'wide' ? render3d(ctx) : slot === 'a' ? wireCube(ctx) : clay(ctx);
      case 'foto-video': {
        const kinds = ['landscape', 'city', 'product', 'portrait'];
        const base = project.photo || 'landscape';
        const others = kinds.filter((k) => k !== base && k !== 'portrait');
        const kind = slot === 'a' ? 'portrait' : slot === 'b' ? others[project.seed % others.length] : base;
        return photo({ ...ctx, kind, video: project.hasVideo && slot === 'main' });
      }
      default:
        return null;
    }
  }, [project, slot, idx, W, H, uid, real]);

  if (real) {
    return <img className={`part ${className}`} src={real.startsWith('http') ? real : asset(real)} alt="" loading="lazy" />;
  }

  return (
    <svg
      className={`part ${className}`}
      viewBox={`0 0 ${W} ${H}`}
      preserveAspectRatio="xMidYMid slice"
      role="img"
      aria-label={`Ilustración del proyecto ${project.title.join(' ')}`}
    >
      <defs>
        <filter id={`noise-${uid}`} x="0" y="0" width="100%" height="100%">
          <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed={project.seed} />
          <feColorMatrix values="0 0 0 0 0.5  0 0 0 0 0.5  0 0 0 0 0.5  0 0 0 0.9 0" />
        </filter>
      </defs>
      {content}
    </svg>
  );
}

const Noise = ({ uid, W, H, o = 0.18 }) => (
  <rect width={W} height={H} filter={`url(#noise-${uid})`} opacity={o} style={{ mixBlendMode: 'overlay' }} />
);

// ── VIDEOJUEGOS: pixel art ───────────────────────────────────
const SPRITE = [
  '...1111...',
  '..111111..',
  '..122121..',
  '..122221..',
  '...1111...',
  '.11111111.',
  '1.111111.1',
  '..11..11..',
  '..11..11..',
  '.111..111.',
];
function sprite(x, y, u, key, map = SPRITE, light = false) {
  const out = [];
  map.forEach((row, j) =>
    [...row].forEach((c, i) => {
      if (c === '.') return;
      const fill = c === '1' ? (light ? PAPER : INK) : light ? INK : PAPER;
      out.push(<rect key={`${key}${i}-${j}`} x={x + i * u} y={y + j * u} width={u} height={u} fill={fill} />);
    })
  );
  return out;
}
function pixelCircle(cx, cy, rad, u, fill, key) {
  const out = [];
  for (let j = -rad; j <= rad; j++)
    for (let i = -rad; i <= rad; i++)
      if (i * i + j * j <= rad * rad + 1) out.push(<rect key={`${key}${i}_${j}`} x={(cx + i) * u} y={(cy + j) * u} width={u} height={u} fill={fill} />);
  return out;
}
function skyBands(W, H, night, u) {
  const cols = night ? ['#161616', '#1e1e1e', '#282828', '#333'] : ['#f0f0ee', '#e2e2e0', '#d2d2d0', '#c2c2c0'];
  const bh = Math.ceil(H / u / 1.6 / cols.length) * u;
  return cols.map((c, i) => <rect key={`sky${i}`} x={0} y={i * bh} width={W} height={i === cols.length - 1 ? H : bh + u} fill={c} />);
}
function mountains(W, H, u, r, base, fill, key, amp) {
  const cols = Math.ceil(W / u);
  let h = base;
  const out = [];
  for (let i = 0; i < cols; i++) {
    h += Math.round((r() - 0.5) * 3);
    h = Math.max(base - amp, Math.min(base + 2, h));
    out.push(<rect key={`${key}${i}`} x={i * u} y={h * u} width={u} height={H - h * u} fill={fill} />);
  }
  return out;
}
function pixelScene({ W, H, r, uid }) {
  const u = 5;
  const night = r() > 0.5;
  const rows = H / u;
  const ground = rows - 7;
  const els = [...skyBands(W, H, night, u)];
  if (night) for (let i = 0; i < 40; i++) els.push(<rect key={`st${i}`} x={Math.floor(r() * (W / u)) * u} y={Math.floor(r() * (rows * 0.5)) * u} width={u * 0.6} height={u * 0.6} fill={PAPER} opacity={0.4 + r() * 0.6} />);
  els.push(...pixelCircle(Math.floor(W / u * (0.6 + r() * 0.3)), 8, 4, u, night ? '#e8e8e8' : '#ffffff', 'sun'));
  if (!night)
    for (let c = 0; c < 3; c++) {
      const cx = Math.floor(r() * (W / u - 12)), cy = 3 + Math.floor(r() * 6);
      els.push(<rect key={`cl${c}a`} x={cx * u} y={cy * u} width={10 * u} height={2 * u} fill="#fff" />, <rect key={`cl${c}b`} x={(cx + 2) * u} y={(cy - 1) * u} width={5 * u} height={u} fill="#fff" />);
    }
  els.push(...mountains(W, H, u, r, ground - 9, night ? '#3a3a3a' : '#a9a9a7', 'm1', 8));
  els.push(...mountains(W, H, u, r, ground - 4, night ? '#262626' : '#7d7d7b', 'm2', 5));
  // suelo de ladrillos
  els.push(<rect key="g" x={0} y={ground * u} width={W} height={H} fill="#1c1c1c" />);
  els.push(<rect key="gt" x={0} y={ground * u} width={W} height={u} fill={night ? '#5a5a5a' : '#3a3a3a'} />);
  for (let j = 1; j < 7; j++)
    for (let i = 0; i < W / u / 3 + 1; i++)
      els.push(<rect key={`b${i}-${j}`} x={i * 3 * u + (j % 2 ? u * 1.5 : 0)} y={(ground + j) * u} width={u * 0.4} height={u} fill="#0b0b0b" />);
  // plataformas y monedas
  for (let p = 0; p < 3; p++) {
    const px = 16 + p * Math.floor(W / u / 3.2) + Math.floor(r() * 4);
    const py = ground - 7 - Math.floor(r() * 7);
    const len = 5 + Math.floor(r() * 4);
    els.push(<rect key={`p${p}`} x={px * u} y={py * u} width={len * u} height={u * 1.4} fill={night ? '#d9d9d9' : '#262626'} />);
    for (let c = 0; c < 3; c++) els.push(<rect key={`c${p}${c}`} x={(px + 1 + c * 2) * u} y={(py - 3) * u} width={u} height={u * 1.4} fill={PAPER} stroke={INK} strokeWidth={0.8} />);
  }
  els.push(...sprite(8 * u, (ground - 10) * u, u, 'hero', SPRITE, night));
  els.push(...sprite((W / u - 16) * u, (ground - 5) * u, u * 0.5 * 2, 'enemy', ['.111.', '11211', '11111', '1.1.1'], night));
  return <g shapeRendering="crispEdges">{els}</g>;
}
function pixelTower({ W, H, r }) {
  const u = 5;
  const night = r() > 0.4;
  const els = [...skyBands(W, H, night, u)];
  const rows = H / u;
  els.push(...pixelCircle(6, 7, 3, u, '#fff', 'moon'));
  els.push(...mountains(W, H, u, r, rows - 12, night ? '#303030' : '#9c9c9a', 'tm', 6));
  const tx = 14, tw = 12;
  els.push(<rect key="tw" x={tx * u} y={8 * u} width={tw * u} height={H} fill="#141414" />);
  els.push(<rect key="tr" x={(tx - 1) * u} y={6 * u} width={(tw + 2) * u} height={2 * u} fill="#0b0b0b" />);
  for (let i = 0; i < 4; i++) els.push(<rect key={`cr${i}`} x={(tx - 1 + i * 4) * u} y={4 * u} width={2 * u} height={2 * u} fill="#0b0b0b" />);
  for (let j = 0; j < 8; j++)
    for (let i = 0; i < 3; i++)
      if (r() > 0.35) els.push(<rect key={`w${i}${j}`} x={(tx + 2 + i * 3.5) * u} y={(11 + j * 3.5) * u} width={u * 1.4} height={u * 2} fill={r() > 0.5 ? '#e6e6e6' : '#555'} />);
  for (let p = 0; p < 4; p++) els.push(<rect key={`lp${p}`} x={(p % 2 ? tx + tw : tx - 6) * u} y={(14 + p * 7) * u} width={6 * u} height={u} fill={PAPER} />);
  els.push(...sprite((tx + tw + 1) * u, (14 + 7 - 5) * u, u * 0.5, 'mini', SPRITE, true));
  return <g shapeRendering="crispEdges">{els}</g>;
}
function pixelHero({ W, H, r }) {
  const u = 10;
  const light = r() > 0.5;
  const els = [<rect key="bg" width={W} height={H} fill={light ? '#e9e9e7' : '#151515'} />];
  for (let i = 0; i < 12; i++) els.push(<rect key={`d${i}`} x={Math.floor(r() * 16) * u} y={Math.floor(r() * 20) * u} width={u / 2} height={u / 2} fill={light ? '#bdbdbd' : '#3a3a3a'} />);
  els.push(<rect key="fl" x={0} y={H - 4 * u} width={W} height={4 * u} fill={light ? '#1a1a1a' : '#e5e5e5'} />);
  els.push(...sprite(3 * u, H - 14 * u, u, 'big', SPRITE, !light));
  // barra de vida
  els.push(<rect key="hp" x={u} y={u} width={9 * u} height={u * 1.2} fill="none" stroke={light ? INK : PAPER} strokeWidth={2} />);
  els.push(<rect key="hpf" x={u + 3} y={u + 3} width={(9 * u - 6) * (0.4 + r() * 0.5)} height={u * 1.2 - 6} fill={light ? INK : PAPER} />);
  for (let i = 0; i < 3; i++) els.push(...pixelHeart(11 + i * 1.6, 1, u * 0.5, light, `h${i}`));
  return <g shapeRendering="crispEdges">{els}</g>;
}
function pixelHeart(cx, cy, u, light, key) {
  return sprite(cx * u * 2, cy * u * 2, u, key, ['.1.1.', '11111', '11111', '.111.', '..1..'], !light);
}

// ── PÁGINAS WEB ─────────────────────────────────────────────
function webDesktop({ W, H, r, uid }) {
  const dark = r() > 0.5;
  const els = [];
  els.push(<rect key="bg" width={W} height={H} fill="#cfcfcd" />);
  for (let i = 0; i < 18; i++) for (let j = 0; j < 12; j++) els.push(<circle key={`dt${i}-${j}`} cx={i * 18 + 9} cy={j * 18 + 9} r={0.8} fill="#b5b5b3" />);
  els.push(
    <g key="win" style={{ filter: 'drop-shadow(0 8px 12px rgba(0,0,0,.35))' }}>
      <rect x={22} y={18} width={W - 44} height={H} rx={8} fill={PAPER} />
      <rect x={22} y={18} width={W - 44} height={14} rx={8} fill={INK} />
      <rect x={22} y={26} width={W - 44} height={6} fill={INK} />
      {[0, 1, 2].map((i) => (
        <circle key={i} cx={32 + i * 8} cy={25} r={2.2} fill={i === 0 ? PAPER : '#777'} />
      ))}
      <rect x={110} y={21.5} width={100} height={7} rx={3.5} fill="#333" />
    </g>
  );
  const x0 = 34, y0 = 42, w = W - 68;
  els.push(<rect key="logo" x={x0} y={y0} width={22} height={6} rx={2} fill={INK} />);
  [0, 1, 2, 3].forEach((i) => els.push(<rect key={`nl${i}`} x={x0 + w - 110 + i * 26} y={y0 + 1} width={18} height={4} rx={2} fill="#9a9a9a" />));
  const hy = y0 + 16;
  if (dark) {
    els.push(<rect key="hero" x={x0} y={hy} width={w} height={82} rx={6} fill={INK} />);
    els.push(<rect key="h1" x={x0 + 14} y={hy + 16} width={w * 0.42} height={11} rx={2} fill={PAPER} />);
    els.push(<rect key="h2" x={x0 + 14} y={hy + 31} width={w * 0.32} height={11} rx={2} fill={PAPER} />);
    els.push(<rect key="h3" x={x0 + 14} y={hy + 50} width={w * 0.36} height={4} rx={2} fill="#777" />);
    els.push(<rect key="btn" x={x0 + 14} y={hy + 62} width={42} height={11} rx={5.5} fill={PAPER} />);
    els.push(<circle key="img" cx={x0 + w * 0.76} cy={hy + 41} r={30} fill="#3a3a3a" />);
    els.push(<circle key="img2" cx={x0 + w * 0.76} cy={hy + 41} r={18} fill="#8a8a8a" />);
  } else {
    els.push(<rect key="h1" x={x0 + w * 0.18} y={hy + 8} width={w * 0.64} height={13} rx={2} fill={INK} />);
    els.push(<rect key="h2" x={x0 + w * 0.26} y={hy + 25} width={w * 0.48} height={13} rx={2} fill={INK} />);
    els.push(<rect key="h3" x={x0 + w * 0.3} y={hy + 45} width={w * 0.4} height={4} rx={2} fill="#9a9a9a" />);
    els.push(<rect key="btn" x={x0 + w / 2 - 24} y={hy + 56} width={48} height={12} rx={6} fill={INK} />);
    els.push(<rect key="img" x={x0} y={hy + 76} width={w} height={12} rx={3} fill="#dcdcda" />);
  }
  const cy = hy + 94;
  const n = 3;
  for (let i = 0; i < n; i++) {
    const cw = (w - (n - 1) * 8) / n, cx = x0 + i * (cw + 8);
    els.push(<rect key={`c${i}`} x={cx} y={cy} width={cw} height={60} rx={5} fill="#e3e3e1" />);
    els.push(<rect key={`ci${i}`} x={cx + 6} y={cy + 6} width={cw - 12} height={26} rx={3} fill={i === 1 ? INK : '#bdbdbb'} />);
    els.push(<rect key={`ct${i}`} x={cx + 6} y={cy + 38} width={cw * 0.6} height={4} rx={2} fill={INK} />);
  }
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.1} />);
  return els;
}
function webPhone({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill={r() > 0.5 ? '#1a1a1a' : '#bdbdbb'} />];
  const pw = 92, ph = 186, px = (W - pw) / 2, py = 24;
  els.push(
    <g key="ph" transform={`rotate(-6 ${W / 2} ${H / 2})`} style={{ filter: 'drop-shadow(0 10px 14px rgba(0,0,0,.5))' }}>
      <rect x={px} y={py} width={pw} height={ph} rx={16} fill={INK} />
      <rect x={px + 5} y={py + 5} width={pw - 10} height={ph - 10} rx={12} fill={PAPER} />
      <rect x={px + pw / 2 - 14} y={py + 9} width={28} height={6} rx={3} fill={INK} />
      <rect x={px + 12} y={py + 26} width={40} height={7} rx={2} fill={INK} />
      <rect x={px + 12} y={py + 36} width={56} height={7} rx={2} fill={INK} />
      <rect x={px + 12} y={py + 50} width={pw - 24} height={52} rx={6} fill="#2a2a2a" />
      <circle cx={px + pw / 2} cy={py + 76} r={14} fill="#8a8a8a" />
      {[0, 1, 2].map((i) => (
        <g key={i}>
          <rect x={px + 12} y={py + 110 + i * 20} width={14} height={14} rx={4} fill="#cfcfcd" />
          <rect x={px + 32} y={py + 112 + i * 20} width={36} height={4} rx={2} fill={INK} />
          <rect x={px + 32} y={py + 119 + i * 20} width={24} height={3} rx={1.5} fill="#9a9a9a" />
        </g>
      ))}
      <rect x={px + 12} y={py + 168} width={pw - 24} height={10} rx={5} fill={INK} />
    </g>
  );
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.12} />);
  return els;
}
function webComponents({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill="#e7e7e5" />];
  els.push(
    <text key="aa" x={14} y={78} fontFamily="Lilita One, sans-serif" fontSize={74} fill={INK}>
      Aa
    </text>
  );
  els.push(<rect key="b1" x={14} y={98} width={96} height={24} rx={12} fill={INK} />);
  els.push(<rect key="b1t" x={34} y={108} width={56} height={4} rx={2} fill={PAPER} />);
  els.push(<rect key="b2" x={14} y={130} width={96} height={24} rx={12} fill="none" stroke={INK} strokeWidth={2} />);
  els.push(<rect key="t1" x={14} y={166} width={30} height={16} rx={8} fill={INK} />);
  els.push(<circle key="t1c" cx={36} cy={174} r={6} fill={PAPER} />);
  els.push(<rect key="t2" x={52} y={166} width={30} height={16} rx={8} fill="#bdbdbb" />);
  els.push(<circle key="t2c" cx={60} cy={174} r={6} fill={PAPER} />);
  els.push(
    <path key="cur" d={`M118 140 l0 26 l7 -6 l5 11 l5 -2 l-5 -11 l9 0 z`} fill={INK} stroke={PAPER} strokeWidth={1.5} transform={`rotate(${-10 + r() * 20} 125 150)`} />
  );
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.1} />);
  return els;
}

// ── SOFTWARE ────────────────────────────────────────────────
function dashboard({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill="#101010" />];
  els.push(<rect key="sb" x={0} y={0} width={46} height={H} fill="#1b1b1b" />);
  els.push(<rect key="lg" x={12} y={12} width={22} height={22} rx={6} fill={PAPER} />);
  for (let i = 0; i < 6; i++) els.push(<rect key={`si${i}`} x={16} y={52 + i * 22} width={14} height={14} rx={4} fill={i === 1 ? PAPER : '#3a3a3a'} />);
  els.push(<rect key="tb" x={58} y={12} width={120} height={9} rx={3} fill="#3a3a3a" />);
  els.push(<circle key="av" cx={W - 20} cy={17} r={8} fill="#8a8a8a" />);
  const kx = 58, ky = 32, kw = (W - kx - 12 - 16) / 3;
  const nums = ['$48.2K', '1,284', '97%', '312', '+18%', '24/7'];
  for (let i = 0; i < 3; i++) {
    const x = kx + i * (kw + 8);
    els.push(<rect key={`k${i}`} x={x} y={ky} width={kw} height={42} rx={7} fill={i === 0 ? PAPER : '#1f1f1f'} />);
    els.push(<rect key={`kl${i}`} x={x + 8} y={ky + 8} width={30} height={4} rx={2} fill={i === 0 ? '#777' : '#555'} />);
    els.push(
      <text key={`kt${i}`} x={x + 8} y={ky + 32} fontFamily="Lilita One, sans-serif" fontSize={16} fill={i === 0 ? INK : PAPER}>
        {nums[Math.floor(r() * nums.length)]}
      </text>
    );
  }
  const cy = 84, ch = 104;
  els.push(<rect key="ch" x={kx} y={cy} width={W - kx - 12 - 90} height={ch} rx={8} fill="#1a1a1a" />);
  const bars = 12, bw = (W - kx - 12 - 90 - 24) / bars;
  for (let i = 0; i < bars; i++) {
    const h = 20 + r() * 60;
    els.push(<rect key={`b${i}`} x={kx + 12 + i * bw} y={cy + ch - 12 - h} width={bw * 0.55} height={h} rx={2} fill={i === bars - 3 ? PAPER : '#5a5a5a'} />);
  }
  let d = '';
  for (let i = 0; i < bars; i++) d += `${i ? 'L' : 'M'}${kx + 14 + i * bw} ${cy + 26 + r() * 30} `;
  els.push(<path key="ln" d={d} fill="none" stroke={PAPER} strokeWidth={2} strokeLinejoin="round" />);
  const tx = W - 12 - 82;
  els.push(<rect key="tbl" x={tx} y={cy} width={82} height={ch} rx={8} fill="#1a1a1a" />);
  for (let i = 0; i < 6; i++) {
    els.push(<circle key={`tc${i}`} cx={tx + 12} cy={cy + 14 + i * 15} r={4} fill={i % 2 ? '#5a5a5a' : '#bdbdbd'} />);
    els.push(<rect key={`tr${i}`} x={tx + 22} y={cy + 12 + i * 15} width={30 + r() * 20} height={4} rx={2} fill="#4a4a4a" />);
  }
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.1} />);
  return els;
}
function appList({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill="#dededc" />];
  els.push(<rect key="card" x={14} y={16} width={W - 28} height={H - 32} rx={14} fill={INK} style={{ filter: 'drop-shadow(0 8px 10px rgba(0,0,0,.35))' }} />);
  els.push(<rect key="t" x={26} y={30} width={60} height={9} rx={3} fill={PAPER} />);
  for (let i = 0; i < 6; i++) {
    const y = 52 + i * 22;
    els.push(<circle key={`a${i}`} cx={34} cy={y + 6} r={7} fill={['#f5f5f3', '#8a8a8a', '#4a4a4a'][i % 3]} />);
    els.push(<rect key={`l${i}`} x={48} y={y + 1} width={46 + r() * 20} height={4} rx={2} fill="#cfcfcf" />);
    els.push(<rect key={`s${i}`} x={48} y={y + 8} width={30} height={3} rx={1.5} fill="#5a5a5a" />);
    const on = r() > 0.4;
    els.push(<rect key={`tg${i}`} x={W - 48} y={y} width={20} height={11} rx={5.5} fill={on ? PAPER : '#3a3a3a'} />);
    els.push(<circle key={`tk${i}`} cx={on ? W - 33.5 : W - 42.5} cy={y + 5.5} r={4} fill={on ? INK : '#8a8a8a'} />);
  }
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.1} />);
  return els;
}
function donut({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill="#141414" />];
  const cx = W / 2, cy = 82, R = 46;
  const parts = [0.42, 0.28, 0.18, 0.12];
  const cols = [PAPER, '#9a9a9a', '#5a5a5a', '#2e2e2e'];
  let a0 = -Math.PI / 2;
  parts.forEach((p, i) => {
    const a1 = a0 + p * Math.PI * 2;
    const large = p > 0.5 ? 1 : 0;
    const d = `M${cx + R * Math.cos(a0)} ${cy + R * Math.sin(a0)} A${R} ${R} 0 ${large} 1 ${cx + R * Math.cos(a1)} ${cy + R * Math.sin(a1)}`;
    els.push(<path key={`d${i}`} d={d} fill="none" stroke={cols[i]} strokeWidth={18} />);
    a0 = a1;
  });
  els.push(
    <text key="pc" x={cx} y={cy + 7} textAnchor="middle" fontFamily="Lilita One, sans-serif" fontSize={20} fill={PAPER}>
      {Math.round(60 + r() * 35)}%
    </text>
  );
  parts.forEach((_, i) => {
    els.push(<rect key={`lg${i}`} x={24} y={148 + i * 12} width={8} height={8} rx={2} fill={cols[i]} />);
    els.push(<rect key={`lt${i}`} x={38} y={150 + i * 12} width={50 + r() * 40} height={4} rx={2} fill="#4a4a4a" />);
  });
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.1} />);
  return els;
}

// ── DISEÑO GRÁFICO ──────────────────────────────────────────
function poster({ W, H, r, uid, project }) {
  const inv = r() > 0.5;
  const bg = inv ? INK : '#ecece9', fg = inv ? PAPER : INK;
  const letter = project.title[0].replace(/[^A-Za-zÁÉÍÓÚÑ]/g, '')[0] || 'B';
  const els = [<rect key="bg" width={W} height={H} fill={bg} />];
  els.push(
    <pattern key="ht" id={`ht-${uid}`} width={7} height={7} patternUnits="userSpaceOnUse">
      <circle cx={3.5} cy={3.5} r={1.6} fill={inv ? '#4a4a4a' : '#b4b4b1'} />
    </pattern>
  );
  els.push(<circle key="c1" cx={W * (0.62 + r() * 0.15)} cy={H * 0.45} r={70} fill={`url(#ht-${uid})`} />);
  els.push(<circle key="c2" cx={W * 0.25} cy={H * 0.78} r={26} fill="none" stroke={fg} strokeWidth={3} />);
  for (let i = 1; i < 6; i++) els.push(<line key={`gl${i}`} x1={(W / 6) * i} y1={0} x2={(W / 6) * i} y2={H} stroke={inv ? '#1f1f1f' : '#dcdcd9'} strokeWidth={1} />);
  els.push(
    <text key="L" x={W * 0.08} y={H * 0.86} fontFamily="Lilita One, sans-serif" fontSize={H * 1.02} fill={fg} transform={`rotate(${-8 + r() * 6} ${W * 0.3} ${H * 0.5})`}>
      {letter}
    </text>
  );
  els.push(
    <text key="t" x={W - 14} y={24} textAnchor="end" fontFamily="Lilita One, sans-serif" fontSize={14} fill={fg} letterSpacing={2}>
      {project.title[0].replace(':', '').toUpperCase()}
    </text>
  );
  els.push(<rect key="r1" x={W - 90} y={H - 30} width={76} height={4} fill={fg} />);
  els.push(<rect key="r2" x={W - 90} y={H - 22} width={50} height={4} fill={fg} opacity={0.5} />);
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.16} />);
  return els;
}
function cards({ W, H, r, uid, project }) {
  const els = [<rect key="bg" width={W} height={H} fill="#8f8f8d" />];
  const letter = project.title[0][0];
  const card = (x, y, rot, dark, k) => (
    <g key={k} transform={`rotate(${rot} ${x + 50} ${y + 30})`} style={{ filter: 'drop-shadow(0 8px 8px rgba(0,0,0,.4))' }}>
      <rect x={x} y={y} width={100} height={60} rx={4} fill={dark ? INK : PAPER} />
      <circle cx={x + 22} cy={y + 30} r={12} fill={dark ? PAPER : INK} />
      <text x={x + 22} y={y + 35.5} textAnchor="middle" fontFamily="Lilita One, sans-serif" fontSize={15} fill={dark ? INK : PAPER}>
        {letter}
      </text>
      <rect x={x + 42} y={y + 24} width={44} height={5} rx={2} fill={dark ? PAPER : INK} />
      <rect x={x + 42} y={y + 33} width={30} height={3} rx={1.5} fill="#8a8a8a" />
    </g>
  );
  els.push(card(22, 44, -12 + r() * 4, true, 'k1'));
  els.push(card(36, 104, 8 - r() * 4, false, 'k2'));
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.18} />);
  return els;
}
function swatches({ W, H, uid }) {
  const cols = ['#0b0b0b', '#3a3a3a', '#7a7a7a', '#bdbdbd', '#f5f5f3'];
  const els = cols.map((c, i) => <rect key={i} x={0} y={(H / cols.length) * i} width={W} height={H / cols.length + 1} fill={c} />);
  cols.forEach((c, i) =>
    els.push(
      <text key={`t${i}`} x={12} y={(H / cols.length) * i + 24} fontFamily="Baloo 2, sans-serif" fontWeight={800} fontSize={10} fill={i < 3 ? PAPER : INK}>
        {c.toUpperCase()}
      </text>
    )
  );
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.12} />);
  return els;
}

// ── MODELADO 3D ─────────────────────────────────────────────
function render3d({ W, H, uid, project, r }) {
  const els = [];
  els.push(
    <radialGradient key="rg" id={`rg-${uid}`} cx="50%" cy="40%" r="75%">
      <stop offset="0" stopColor="#e6e6e4" />
      <stop offset="0.6" stopColor="#9a9a98" />
      <stop offset="1" stopColor="#4a4a4a" />
    </radialGradient>
  );
  els.push(<rect key="bg" width={W} height={H} fill={`url(#rg-${uid})`} />);
  for (let i = -8; i <= 8; i++) els.push(<line key={`gx${i}`} x1={W / 2 + i * 12} y1={H * 0.72} x2={W / 2 + i * 60} y2={H} stroke="rgba(255,255,255,.35)" strokeWidth={0.8} />);
  for (let j = 0; j < 5; j++) els.push(<line key={`gy${j}`} x1={0} y1={H * 0.72 + j * j * 3 + j * 4} x2={W} y2={H * 0.72 + j * j * 3 + j * 4} stroke="rgba(255,255,255,.3)" strokeWidth={0.8} />);
  els.push(<ellipse key="sh" cx={W / 2} cy={H * 0.83} rx={62} ry={9} fill="rgba(0,0,0,.35)" />);
  const img = project.render || 'renders/modeling.webp';
  const s = 0.62 + r() * 0.06;
  els.push(<image key="im" href={asset(img)} x={W / 2 - (H * s) / 2} y={H * 0.1} width={H * s} height={H * s * 1.05} preserveAspectRatio="xMidYMid meet" />);
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.12} />);
  return els;
}
function wireCube({ W, H, r, uid }) {
  const els = [<rect key="bg" width={W} height={H} fill="#121212" />];
  const cx = W / 2, cy = H / 2 + 6, s = 44;
  const iso = (x, y, z) => [cx + (x - z) * s * 0.87, cy + (x + z) * s * 0.5 - y * s];
  const pts = [];
  for (const x of [-0.5, 0.5]) for (const y of [-0.5, 0.5]) for (const z of [-0.5, 0.5]) pts.push([x, y, z]);
  const edges = [];
  pts.forEach((a, i) => pts.forEach((b, j) => {
    if (j > i && Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) === 1) edges.push([a, b]);
  }));
  // subdivisiones
  const lines = [];
  for (let t = -0.5; t <= 0.5001; t += 0.25) {
    lines.push([[t, 0.5, -0.5], [t, 0.5, 0.5]], [[-0.5, 0.5, t], [0.5, 0.5, t]]);
    lines.push([[t, -0.5, 0.5], [t, 0.5, 0.5]], [[-0.5, t, 0.5], [0.5, t, 0.5]]);
    lines.push([[0.5, -0.5, t], [0.5, 0.5, t]], [[0.5, t, -0.5], [0.5, t, 0.5]]);
  }
  lines.forEach(([a, b], i) => {
    const [x1, y1] = iso(...a), [x2, y2] = iso(...b);
    els.push(<line key={`s${i}`} x1={x1} y1={y1} x2={x2} y2={y2} stroke="#4a4a4a" strokeWidth={0.8} />);
  });
  edges.forEach(([a, b], i) => {
    const [x1, y1] = iso(...a), [x2, y2] = iso(...b);
    els.push(<line key={`e${i}`} x1={x1} y1={y1} x2={x2} y2={y2} stroke={PAPER} strokeWidth={1.6} />);
  });
  pts.forEach((p, i) => {
    const [x, y] = iso(...p);
    els.push(<rect key={`v${i}`} x={x - 2.5} y={y - 2.5} width={5} height={5} fill={i === Math.floor(r() * 8) ? PAPER : INK} stroke={PAPER} strokeWidth={1} />);
  });
  els.push(<text key="t" x={12} y={H - 12} fontFamily="Baloo 2, sans-serif" fontWeight={800} fontSize={9} fill="#6a6a6a">VERTS 8 · FACES 6</text>);
  return els;
}
function clay({ W, H, uid, r }) {
  const els = [<rect key="bg" width={W} height={H} fill="#d6d6d4" />];
  els.push(
    <radialGradient key="g1" id={`cg-${uid}`} cx="35%" cy="30%" r="70%">
      <stop offset="0" stopColor="#ffffff" />
      <stop offset="0.55" stopColor="#bdbdbb" />
      <stop offset="1" stopColor="#4a4a4a" />
    </radialGradient>,
    <radialGradient key="g2" id={`cd-${uid}`} cx="35%" cy="30%" r="70%">
      <stop offset="0" stopColor="#8a8a8a" />
      <stop offset="0.5" stopColor="#1f1f1f" />
      <stop offset="1" stopColor="#000" />
    </radialGradient>
  );
  const balls = [
    [W * 0.35, H * 0.62, 34, 'cd'],
    [W * 0.68, H * 0.7, 24, 'cg'],
    [W * 0.55, H * 0.36, 28, 'cg'],
  ];
  balls.forEach(([x, y, rad, g], i) => {
    els.push(<ellipse key={`s${i}`} cx={x + 6} cy={y + rad * 0.95} rx={rad * 0.9} ry={rad * 0.22} fill="rgba(0,0,0,.25)" />);
    els.push(<circle key={`b${i}`} cx={x} cy={y - r() * 2} r={rad} fill={`url(#${g}-${uid})`} />);
  });
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.14} />);
  return els;
}

// ── FOTOGRAFÍA Y VIDEO ──────────────────────────────────────
function ridge(W, base, amp, r, steps = 14) {
  let d = `M0 ${base}`;
  for (let i = 1; i <= steps; i++) {
    const x = (W / steps) * i;
    const y = base - r() * amp;
    const cx = x - W / steps / 2;
    d += ` Q${cx} ${y - amp * 0.3} ${x} ${y}`;
  }
  return d;
}
function photo({ W, H, r, uid, kind, video }) {
  const els = [];
  if (kind === 'landscape') {
    els.push(
      <linearGradient key="sky" id={`sk-${uid}`} x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#2a2a2a" />
        <stop offset="0.55" stopColor="#bdbdbb" />
        <stop offset="1" stopColor="#f0f0ee" />
      </linearGradient>
    );
    els.push(<rect key="bg" width={W} height={H} fill={`url(#sk-${uid})`} />);
    const sx = W * (0.3 + r() * 0.4);
    els.push(<circle key="glow" cx={sx} cy={H * 0.55} r={60} fill="#fff" opacity={0.35} style={{ filter: 'blur(8px)' }} />);
    els.push(<circle key="sun" cx={sx} cy={H * 0.56} r={16} fill="#fff" />);
    const layers = [['#8a8a88', 0.62, 40], ['#4a4a4a', 0.72, 34], ['#1c1c1c', 0.84, 26], ['#080808', 0.95, 18]];
    layers.forEach(([c, b, a], i) => els.push(<path key={`r${i}`} d={`${ridge(W, H * b, a, r)} L${W} ${H} L0 ${H} Z`} fill={c} />));
  } else if (kind === 'city') {
    els.push(
      <linearGradient key="sky" id={`sk-${uid}`} x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#d9d9d7" />
        <stop offset="1" stopColor="#8a8a88" />
      </linearGradient>
    );
    els.push(<rect key="bg" width={W} height={H} fill={`url(#sk-${uid})`} />);
    let x = -10;
    let i = 0;
    while (x < W) {
      const bw = 20 + r() * 34, bh = H * (0.3 + r() * 0.55);
      const c = ['#1a1a1a', '#2e2e2e', '#0e0e0e', '#3c3c3c'][i % 4];
      els.push(<rect key={`bd${i}`} x={x} y={H - bh} width={bw} height={bh} fill={c} />);
      for (let wy = H - bh + 8; wy < H - 10; wy += 9)
        for (let wx = x + 4; wx < x + bw - 5; wx += 7)
          if (r() > 0.55) els.push(<rect key={`w${i}-${wx}-${wy}`} x={wx} y={wy} width={3} height={4} fill="#e8e8e6" opacity={0.7} />);
      x += bw + 2;
      i++;
    }
    els.push(<rect key="fog" x={0} y={H * 0.55} width={W} height={H * 0.45} fill="#fff" opacity={0.08} />);
  } else if (kind === 'product') {
    els.push(
      <radialGradient key="spot" id={`sp-${uid}`} cx="50%" cy="35%" r="70%">
        <stop offset="0" stopColor="#6a6a6a" />
        <stop offset="0.6" stopColor="#1a1a1a" />
        <stop offset="1" stopColor="#060606" />
      </radialGradient>,
      <linearGradient key="bt" id={`bt-${uid}`} x1="0" x2="1">
        <stop offset="0" stopColor="#0a0a0a" />
        <stop offset="0.3" stopColor="#6a6a6a" />
        <stop offset="0.45" stopColor="#1a1a1a" />
        <stop offset="1" stopColor="#050505" />
      </linearGradient>
    );
    els.push(<rect key="bg" width={W} height={H} fill={`url(#sp-${uid})`} />);
    const cx = W / 2, top = H * 0.82;
    els.push(<ellipse key="pd" cx={cx} cy={top} rx={W * 0.28} ry={10} fill="#d9d9d7" />);
    els.push(<rect key="pb" x={cx - W * 0.28} y={top} width={W * 0.56} height={H} fill="#8a8a88" />);
    els.push(<ellipse key="pd2" cx={cx} cy={top} rx={W * 0.28} ry={10} fill="#e8e8e6" />);
    const bw = 34;
    els.push(
      <path
        key="bottle"
        d={`M${cx - 7} ${H * 0.28} h14 v16 c0 6 ${bw / 2 - 7} 10 ${bw / 2 - 7} 22 v${H * 0.34} c0 5 -4 8 -8 8 h${-bw + 16} c-4 0 -8 -3 -8 -8 v${-H * 0.34} c0 -12 ${bw / 2 - 7} -16 ${bw / 2 - 7} -22 z`}
        fill={`url(#bt-${uid})`}
      />
    );
    els.push(<rect key="lbl" x={cx - bw / 2 + 3} y={H * 0.5} width={bw - 6} height={22} fill={PAPER} opacity={0.9} />);
    els.push(<rect key="cap" x={cx - 8} y={H * 0.22} width={16} height={10} rx={2} fill="#bdbdbd" />);
  } else {
    // retrato a contraluz
    els.push(
      <radialGradient key="rl" id={`rl-${uid}`} cx="65%" cy="40%" r="60%">
        <stop offset="0" stopColor="#bdbdbb" />
        <stop offset="0.5" stopColor="#3a3a3a" />
        <stop offset="1" stopColor="#0a0a0a" />
      </radialGradient>
    );
    els.push(<rect key="bg" width={W} height={H} fill={`url(#rl-${uid})`} />);
    const cx = W * 0.46;
    els.push(
      <path
        key="body"
        d={`M${cx - 70} ${H} C${cx - 64} ${H * 0.74} ${cx - 34} ${H * 0.68} ${cx - 16} ${H * 0.64} L${cx - 12} ${H * 0.55} C${cx - 30} ${H * 0.5} ${cx - 32} ${H * 0.26} ${cx} ${H * 0.24} C${cx + 32} ${H * 0.26} ${cx + 30} ${H * 0.5} ${cx + 12} ${H * 0.55} L${cx + 16} ${H * 0.64} C${cx + 34} ${H * 0.68} ${cx + 64} ${H * 0.74} ${cx + 70} ${H} Z`}
        fill="#060606"
        stroke="rgba(255,255,255,.55)"
        strokeWidth={1.4}
      />
    );
  }
  if (video) {
    els.push(<rect key="lb1" x={0} y={0} width={W} height={H * 0.08} fill="#000" />);
    els.push(<rect key="lb2" x={0} y={H * 0.92} width={W} height={H * 0.08} fill="#000" />);
    els.push(<circle key="rec" cx={W - 36} cy={H * 0.04} r={3} fill={PAPER} />);
    els.push(
      <text key="rect" x={W - 30} y={H * 0.04 + 3} fontFamily="Baloo 2, sans-serif" fontWeight={800} fontSize={8} fill={PAPER}>
        REC
      </text>
    );
  }
  els.push(<Noise key="n" uid={uid} W={W} H={H} o={0.28} />);
  return els;
}
