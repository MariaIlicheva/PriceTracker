// График истории цены на SVG без сторонних библиотек.
// По линии на каждую ссылку (цвет магазина) и пунктир порога.
// При наведении показывается ближайший замер: дата, время и цена.

import { formatPrice, formatShortDate, formatWhen, html, mount } from './ui.js';

const PAD = { top: 16, right: 16, bottom: 34, left: 64 };
const DAY = 24 * 3600 * 1000;

/** Круглые деления оси цен: 26 000, 28 000, 30 000… */
function niceTicks(min, max, count = 4) {
    const span = max - min || max || 1;
    const rough = span / count;
    const power = 10 ** Math.floor(Math.log10(rough));
    const step = [1, 2, 2.5, 5, 10].map(k => k * power).find(s => s >= rough);
    const start = Math.floor(min / step) * step;
    const end = Math.ceil(max / step) * step;
    const ticks = [];
    for (let v = start; v <= end + step / 2; v += step) ticks.push(Math.round(v * 100) / 100);
    return ticks;
}

/**
 * @param {HTMLElement} container
 * @param {{from: string, to: string, series: Array<{marketplaceCode: string,
 *          points: Array<{capturedAt: string, price: number}>}>}} history
 * @param {number} threshold порог уведомления
 */
export function renderChart(container, history, threshold) {
    // Рисуем в масштабе 1:1 с шириной блока, чтобы подписи не мельчали на телефоне
    const WIDTH = Math.max(300, Math.round(container.clientWidth));
    const HEIGHT = WIDTH < 500 ? 240 : 300;
    const from = new Date(history.from).getTime();
    const to = new Date(history.to).getTime();
    const series = history.series
        .map(s => ({ ...s, points: s.points.map(p => ({ time: new Date(p.capturedAt).getTime(), price: Number(p.price) })) }))
        .filter(s => s.points.length > 0);

    const prices = series.flatMap(s => s.points.map(p => p.price)).concat(Number(threshold));
    const ticks = niceTicks(Math.min(...prices), Math.max(...prices));
    const yMin = ticks[0];
    const yMax = ticks[ticks.length - 1];

    const x = time => PAD.left + ((time - from) / (to - from)) * (WIDTH - PAD.left - PAD.right);
    const y = price => PAD.top + (1 - (price - yMin) / (yMax - yMin || 1)) * (HEIGHT - PAD.top - PAD.bottom);

    // Подписи дат: не теснее 64px друг от друга
    const pxPerDay = (WIDTH - PAD.left - PAD.right) / ((to - from) / DAY);
    const dayStep = Math.max(1, Math.ceil(64 / pxPerDay));
    const dayTicks = [];
    const first = new Date(from);
    first.setHours(12, 0, 0, 0);
    for (let t = first.getTime(); t <= to; t += dayStep * DAY) {
        if (t >= from) dayTicks.push(t);
    }

    const points = series.flatMap(s => s.points.map(p => ({ ...p, code: s.marketplaceCode, cx: x(p.time), cy: y(p.price) })));

    mount(container, html`
        <svg viewBox="0 0 ${WIDTH} ${HEIGHT}" role="img" aria-label="График цены по магазинам">
            <g class="chart-grid">
                ${ticks.map(t => html`<line x1="${PAD.left}" x2="${WIDTH - PAD.right}" y1="${y(t)}" y2="${y(t)}"/>`)}
            </g>
            <g class="chart-axis">
                ${ticks.map(t => html`<text x="${PAD.left - 12}" y="${y(t) + 4}" text-anchor="end">${formatPrice(t).replace(' ₽', '')}</text>`)}
                ${dayTicks.map(t => html`<text x="${x(t)}" y="${HEIGHT - 8}" text-anchor="middle">${formatShortDate(new Date(t))}</text>`)}
            </g>
            <line class="chart-threshold" x1="${PAD.left}" x2="${WIDTH - PAD.right}" y1="${y(threshold)}" y2="${y(threshold)}"/>
            ${series.map(s => html`
                <polyline class="chart-line mp-${s.marketplaceCode}"
                          points="${s.points.map(p => `${x(p.time).toFixed(1)},${y(p.price).toFixed(1)}`).join(' ')}"/>`)}
            <circle class="chart-focus" r="6" visibility="hidden"/>
            <rect class="chart-hit" x="${PAD.left}" y="0" width="${WIDTH - PAD.left - PAD.right}" height="${HEIGHT}" fill="transparent"/>
        </svg>
        <div class="chart-tooltip" hidden></div>
    `);

    const svg = container.querySelector('svg');
    const focus = container.querySelector('.chart-focus');
    const tooltip = container.querySelector('.chart-tooltip');

    svg.addEventListener('pointermove', event => {
        const box = svg.getBoundingClientRect();
        const scale = WIDTH / box.width;
        const px = (event.clientX - box.left) * scale;
        const py = (event.clientY - box.top) * scale;
        let nearest = null;
        let best = Infinity;
        for (const p of points) {
            const d = (p.cx - px) ** 2 + ((p.cy - py) / 3) ** 2; // по горизонтали точнее, чем по вертикали
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (!nearest) return;
        focus.setAttribute('cx', nearest.cx);
        focus.setAttribute('cy', nearest.cy);
        focus.setAttribute('class', `chart-focus mp-${nearest.code}`);
        focus.setAttribute('visibility', 'visible');
        tooltip.textContent = `${formatWhen(new Date(nearest.time).toISOString())} · ${formatPrice(nearest.price)}`;
        tooltip.style.left = `${nearest.cx / scale}px`;
        tooltip.style.top = `${nearest.cy / scale}px`;
        tooltip.hidden = false;
    });

    svg.addEventListener('pointerleave', () => {
        focus.setAttribute('visibility', 'hidden');
        tooltip.hidden = true;
    });
}
