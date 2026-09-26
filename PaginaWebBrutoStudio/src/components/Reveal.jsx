import { motion } from 'framer-motion';

// Aparición al hacer scroll (sube + se desenfoca a nítido), como en el video.
export default function Reveal({ as = 'div', delay = 0, y = 50, x = 0, scale = 1, className = '', children, amount = 0.3, ...rest }) {
  const Tag = motion[as] || motion.div;
  return (
    <Tag
      className={className}
      initial={{ opacity: 0, y, x, scale, filter: 'blur(8px)' }}
      whileInView={{ opacity: 1, y: 0, x: 0, scale: 1, filter: 'blur(0px)' }}
      viewport={{ once: true, amount }}
      transition={{ duration: 0.9, delay, ease: [0.22, 1, 0.36, 1] }}
      {...rest}
    >
      {children}
    </Tag>
  );
}
