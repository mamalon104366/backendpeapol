import { useRef } from 'react';
import { motion, useScroll, useTransform } from 'framer-motion';
import './FloatCard.css';

// Tarjeta clara con un objeto 3D que "se sale" por arriba (como la botella
// con el pulpo del video) y un lema abajo.
export default function FloatCard({ src, caption = [], tilt = 0, className = '', objClass = '' }) {
  const ref = useRef(null);
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start end', 'end start'] });
  const objY = useTransform(scrollYProgress, [0, 1], ['18%', '-14%']);
  const cardRot = useTransform(scrollYProgress, [0, 1], [tilt + 4, tilt - 4]);

  return (
    <motion.figure
      ref={ref}
      className={`fcard ${className}`}
      style={{ rotate: cardRot }}
      initial={{ opacity: 0, scale: 0.7, y: 80 }}
      whileInView={{ opacity: 1, scale: 1, y: 0 }}
      viewport={{ once: true, amount: 0.35 }}
      transition={{ type: 'spring', stiffness: 120, damping: 16 }}
      whileHover={{ scale: 1.04 }}
    >
      <div className="fcard__window">
        <div className="fcard__glow" />
      </div>
      <motion.img className={`fcard__obj ${objClass}`} src={src} alt="" style={{ y: objY }} />
      <figcaption className="fcard__caption display">
        {caption.map((l) => (
          <span key={l}>{l}</span>
        ))}
      </figcaption>
    </motion.figure>
  );
}
