// Страница товара: статус и действия, график истории цены, таблица ссылок.

import { api, ApiError } from '../api.js';
import { renderChart } from '../chart.js';
import { formatPrice, formatWhen, html, icon, mount, nextCheckText, statePill, toast, withBusy } from '../ui.js';

const PERIODS = [
    { id: 'week', label: 'Неделя' },
    { id: 'month', label: 'Месяц' },
];

let currentPeriod = 'week';

function lastChecked(product) {
    const times = product.links.map(l => l.lastCheckedAt).filter(Boolean).sort();
    return times.length ? times[times.length - 1] : null;
}

function linksTable(product) {
    const target = Number(product.targetPrice);
    return html`
        <ul class="links-table">
            ${product.links.map(link => html`
                <li>
                    <span class="shop">
                        <span class="mp-dot mp-${link.marketplaceCode}"></span>
                        <span class="shop-name">${link.marketplaceName}</span>
                    </span>
                    <span>
                        <span class="price ${link.lastPrice !== null && Number(link.lastPrice) < target ? 'is-below' : ''}">
                            ${formatPrice(link.lastPrice)}
                        </span>
                        <span class="when">· ${formatWhen(link.lastCheckedAt)}</span>
                    </span>
                    <a class="btn btn-sm" href="${link.url}" target="_blank" rel="noopener noreferrer"
                       aria-label="Открыть в магазине ${link.marketplaceName}">
                        <span class="btn-label">Открыть</span> ${icon('external')}
                    </a>
                </li>`)}
        </ul>`;
}

export async function renderProduct(view, ctx, productId) {
    let product = await api.product(productId);
    if (!ctx.isCurrent(ctx.renderId)) return;

    function draw() {
        const paused = product.status === 'paused';
        mount(view, html`
            <a class="back-link" href="#products">${icon('back')} Мои товары</a>
            <div class="page-head">
                <div>
                    <div class="title-line">
                        <h1>${product.name}</h1>
                        ${statePill(product.state)}
                    </div>
                    <p class="muted">
                        Порог уведомления: <strong>${formatPrice(product.targetPrice)}</strong>
                        · последняя проверка ${formatWhen(lastChecked(product))}
                    </p>
                </div>
                <div class="actions">
                    <button class="btn" type="button" id="toggle-status">
                        ${paused ? html`${icon('play')} Возобновить` : html`${icon('pause')} Пауза`}
                    </button>
                    <a class="btn" href="#edit-${product.id}">${icon('edit')} Изменить</a>
                    <button class="btn btn-soft-danger" type="button" id="ask-delete">${icon('trash')} Удалить</button>
                </div>
            </div>

            <div class="stack">
                <div class="confirm" id="confirm-delete" hidden>
                    <p>Удалить «${product.name}»? История цен по ссылкам тоже удалится.</p>
                    <div class="actions">
                        <button class="btn btn-danger" type="button" id="do-delete">Удалить товар</button>
                        <button class="btn" type="button" id="cancel-delete">Отмена</button>
                    </div>
                </div>

                <section class="card">
                    <div class="card-head">
                        <h2>История цены</h2>
                        <div class="segmented" role="group" aria-label="Период графика">
                            ${PERIODS.map(p => html`
                                <button class="tab" type="button" data-period="${p.id}"
                                        aria-pressed="${p.id === currentPeriod}">${p.label}</button>`)}
                        </div>
                    </div>
                    <ul class="legend">
                        ${product.links.map(l => html`<li><span class="mp-dot mp-${l.marketplaceCode}"></span>${l.marketplaceName}</li>`)}
                        <li><span class="threshold-mark"></span>Порог ${formatPrice(product.targetPrice)}</li>
                    </ul>
                    <div class="chart" id="chart"><p class="loading">Загружаем историю…</p></div>
                </section>

                <section class="card">
                    <div class="card-head"><h2>Ссылки</h2></div>
                    ${linksTable(product)}
                </section>
            </div>
        `);

        view.querySelector('#toggle-status').addEventListener('click', event => {
            withBusy(event.currentTarget, async () => {
                try {
                    product = await api.setStatus(product.id, paused ? 'active' : 'paused');
                    toast(paused ? 'Отслеживание возобновлено' : 'Товар на паузе');
                    draw();
                } catch (error) {
                    handleError(error);
                }
            });
        });

        const confirmBox = view.querySelector('#confirm-delete');
        view.querySelector('#ask-delete').addEventListener('click', () => {
            confirmBox.hidden = false;
            confirmBox.querySelector('#do-delete').focus();
        });
        view.querySelector('#cancel-delete').addEventListener('click', () => {
            confirmBox.hidden = true;
        });
        view.querySelector('#do-delete').addEventListener('click', event => {
            withBusy(event.currentTarget, async () => {
                try {
                    await api.deleteProduct(product.id);
                    toast('Товар удалён');
                    ctx.navigate('products');
                } catch (error) {
                    handleError(error);
                }
            });
        });

        view.querySelectorAll('[data-period]').forEach(button => {
            button.addEventListener('click', () => {
                currentPeriod = button.dataset.period;
                view.querySelectorAll('[data-period]').forEach(b => b.setAttribute('aria-pressed', String(b === button)));
                loadHistory();
            });
        });

        loadHistory();
    }

    async function loadHistory() {
        const box = view.querySelector('#chart');
        const period = currentPeriod;

        if (!lastChecked(product)) {
            mount(box, html`<div class="chart-empty">${icon('clock')} Первая проверка цен — ${nextCheckText()}</div>`);
            return;
        }
        try {
            const history = await api.history(product.id, period);
            if (period !== currentPeriod || !box.isConnected) return; // пока ждали, выбрали другой период
            if (history.series.every(s => s.points.length === 0)) {
                mount(box, html`<div class="chart-empty">За этот период замеров нет</div>`);
                return;
            }
            renderChart(box, history, product.targetPrice);
        } catch (error) {
            if (!box.isConnected) return;
            mount(box, html`<div class="chart-empty">Не удалось загрузить историю цен</div>`);
        }
    }

    function handleError(error) {
        if (error instanceof ApiError && error.status === 404) {
            toast('Товар уже удалён', 'error');
            ctx.navigate('products');
            return;
        }
        toast(error.message, 'error');
    }

    draw();
}
