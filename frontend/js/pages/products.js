// Экран «Мои товары»: фильтры по состоянию и список карточек.
// Сервер уже отдаёт товары в нужном порядке (ниже порога — первыми), фильтр работает на клиенте.

import { api } from '../api.js';
import { codeByName, loadMarketplaces } from '../marketplaces.js';
import { formatPrice, html, icon, mount, nextCheckText, plural, statePill } from '../ui.js';

const FILTERS = [
    { id: 'all', label: 'Все' },
    { id: 'below', label: 'Ниже порога' },
    { id: 'tracking', label: 'Отслеживаются' },
    { id: 'paused', label: 'На паузе' },
];

let currentFilter = 'all'; // запоминаем выбор, пока открыта вкладка

/** Подпись под названием: насколько цена далеко от порога. */
function distanceText(product) {
    if (product.bestPrice === null) return 'ждём первую проверку цены';
    const diff = Math.abs(Number(product.targetPrice) - Number(product.bestPrice));
    if (product.state === 'below') return `на ${formatPrice(diff)} ниже порога`;
    return `до порога ${formatPrice(diff)}`;
}

function productItem(product, marketplaces) {
    const code = codeByName(marketplaces, product.bestMarketplace);
    return html`
        <li>
            <a class="product-item is-${product.state}" href="#product-${product.id}">
                <div class="product-main">
                    <span class="product-name">${product.name}</span>
                    <span class="product-meta">
                        ${product.bestMarketplace
                            ? html`<span class="where"><span class="mp-dot mp-${code}"></span>${product.bestMarketplace}</span>`
                            : ''}
                        <span>${distanceText(product)}</span>
                        <span>${product.linkCount} ${plural(product.linkCount, 'ссылка', 'ссылки', 'ссылок')}</span>
                    </span>
                </div>
                <div class="product-side">
                    <span class="price">${formatPrice(product.bestPrice)}</span>
                    ${statePill(product.state)}
                </div>
            </a>
        </li>`;
}

function emptyState(filter) {
    if (filter !== 'all') {
        return html`<div class="card empty"><p class="muted">В этом фильтре пока пусто</p></div>`;
    }
    return html`
        <div class="card empty">
            <span class="bubble empty-bubble" aria-hidden="true"></span>
            <h2>Пока ничего не отслеживаем</h2>
            <p class="muted">Добавь ссылку на товар — и мы напишем, когда цена опустится ниже порога</p>
            <a class="btn btn-primary btn-lg" href="#new">${icon('plus')} Добавить первый товар</a>
        </div>`;
}

export async function renderProducts(view, ctx) {
    const [products, marketplaces] = await Promise.all([api.products(), loadMarketplaces()]);
    if (!ctx.isCurrent(ctx.renderId)) return;

    const counts = { all: products.length };
    for (const p of products) counts[p.state] = (counts[p.state] || 0) + 1;

    function draw() {
        if (products.length === 0) currentFilter = 'all';
        const visible = currentFilter === 'all' ? products : products.filter(p => p.state === currentFilter);
        mount(view, html`
            <div class="page-head">
                <div>
                    <h1>Мои товары</h1>
                    <p class="muted">Следующая проверка цен ${nextCheckText()}</p>
                </div>
                <a class="btn btn-primary btn-lg" href="#new">${icon('plus')} Добавить товар</a>
            </div>

            ${products.length > 0 ? html`
                <div class="tabs" role="group" aria-label="Фильтр товаров">
                    ${FILTERS.map(f => html`
                        <button class="tab" type="button" data-filter="${f.id}" aria-pressed="${f.id === currentFilter}">
                            ${f.label} <span class="count">${counts[f.id] || 0}</span>
                        </button>`)}
                </div>` : ''}

            ${visible.length > 0
                ? html`<ul class="product-list">${visible.map(p => productItem(p, marketplaces))}</ul>`
                : emptyState(currentFilter)}
        `);

        view.querySelectorAll('[data-filter]').forEach(button => {
            button.addEventListener('click', () => {
                currentFilter = button.dataset.filter;
                draw();
            });
        });
    }

    draw();
}
