import { useMemo, useState } from 'react';
import { isDayAvailable, startOfToday, toKey } from '../../utils/booking.js';
import { BOOKING } from '../../config.js';

const DOW = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
const monthFmt = new Intl.DateTimeFormat('es', { month: 'long', year: 'numeric' });

export default function Calendar({ value, onChange, duration }) {
  const today = startOfToday();
  const initial = value ? new Date(value + 'T00:00:00') : today;
  const [view, setView] = useState(new Date(initial.getFullYear(), initial.getMonth(), 1));

  const maxDate = new Date(today);
  maxDate.setDate(maxDate.getDate() + BOOKING.maxDaysAhead);
  const canPrev = view > new Date(today.getFullYear(), today.getMonth(), 1);
  const canNext = new Date(view.getFullYear(), view.getMonth() + 1, 1) <= maxDate;

  const cells = useMemo(() => {
    const first = new Date(view.getFullYear(), view.getMonth(), 1);
    const offset = (first.getDay() + 6) % 7; // lunes primero
    const days = new Date(view.getFullYear(), view.getMonth() + 1, 0).getDate();
    const arr = Array.from({ length: offset }, () => null);
    for (let d = 1; d <= days; d++) {
      const date = new Date(view.getFullYear(), view.getMonth(), d);
      arr.push({ date, key: toKey(date), available: isDayAvailable(date, duration) });
    }
    return arr;
  }, [view, duration]);

  const move = (n) => setView(new Date(view.getFullYear(), view.getMonth() + n, 1));

  return (
    <div className="cal">
      <div className="cal__head">
        <button type="button" onClick={() => move(-1)} disabled={!canPrev} aria-label="Mes anterior">‹</button>
        <strong className="display">{monthFmt.format(view)}</strong>
        <button type="button" onClick={() => move(1)} disabled={!canNext} aria-label="Mes siguiente">›</button>
      </div>
      <div className="cal__grid" role="grid" aria-label="Elige un día">
        {DOW.map((d) => (
          <span key={d} className="cal__dow" aria-hidden="true">{d}</span>
        ))}
        {cells.map((c, i) =>
          c ? (
            <button
              type="button"
              key={c.key}
              className={`cal__day ${value === c.key ? 'is-selected' : ''} ${toKey(today) === c.key ? 'is-today' : ''}`}
              disabled={!c.available}
              aria-pressed={value === c.key}
              aria-label={c.date.toLocaleDateString('es', { weekday: 'long', day: 'numeric', month: 'long' })}
              onClick={() => onChange(c.key)}
            >
              {c.date.getDate()}
            </button>
          ) : (
            <span key={`e${i}`} />
          )
        )}
      </div>
    </div>
  );
}
