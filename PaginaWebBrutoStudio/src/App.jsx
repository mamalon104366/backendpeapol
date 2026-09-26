import { useCallback, useState } from 'react';
import SvgDefs from './components/SvgDefs.jsx';
import Nav from './components/Nav.jsx';
import Hero from './components/Hero.jsx';
import About from './components/About.jsx';
import Mission from './components/Mission.jsx';
import Marquee from './components/Marquee.jsx';
import Services from './components/Services.jsx';
import Projects from './components/Projects.jsx';
import Booking from './components/booking/Booking.jsx';
import Footer from './components/Footer.jsx';
import FloatingCta from './components/FloatingCta.jsx';
import { useLenis, scrollToId } from './hooks/useLenis.js';
import './components/Logo.css';

export default function App() {
  useLenis();
  const [category, setCategory] = useState('videojuegos');
  const [preset, setPreset] = useState(null);

  const seeProjects = useCallback((id) => {
    setCategory(id);
    scrollToId('proyectos');
  }, []);

  const book = useCallback((id) => {
    setPreset({ service: id, n: Date.now() });
    scrollToId('agendar');
  }, []);

  return (
    <>
      <SvgDefs />
      <Nav />
      <main>
        <Hero />
        <About />
        <Mission />
        <Marquee />
        <Services onSeeProjects={seeProjects} onBook={book} />
        <Projects category={category} setCategory={setCategory} onBook={book} />
        <Booking preset={preset} />
      </main>
      <Footer />
      <FloatingCta />
      <div className="grain" aria-hidden="true" />
    </>
  );
}
