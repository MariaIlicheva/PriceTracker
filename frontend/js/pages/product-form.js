// Форма «Новый товар» и «Изменение товара» — одна и та же, отличаются заголовок и запрос.

import { api, ApiError } from '../api.js';
import { detectMarketplace, loadMarketplaces, supportedNames } from '../marketplaces.js';
import {
    clearFieldErrors, formatPrice, html, icon, mount, parsePrice, showFieldErrors, toast, withBusy,
} from '../ui.js';

const MAX_LINKS = 10;

export async function renderProductForm(view, ctx, productId) {
    const isEdit = productId !== null;
    const [marketplaces, product] = await Promise.all([
        loadMarketplaces(),
        isEdit ? api.product(productId) : Promise.resolve(null),
    ]);
    if (!ctx.isCurrent(ctx.renderId)) return;

    const unsupported = `Этот сайт не поддерживается. Доступны: ${supportedNames(marketplaces)}`;
    const links = product ? product.links.map(l => l.url) : [''];
    const backHref = isEdit ? `#product-${productId}` : '#products';

    mount(view, html`
        <a class="back-link" href="${backHref}">${icon('back')} ${isEdit ? 'К товару' : 'Мои товары'}</a>
        <div class="page-head">
            <div>
                <h1>${isEdit ? 'Изменение товара' : 'Новый товар'}</h1>
                <p class="muted">Добавь ссылки на этот товар в разных магазинах. Когда цена хотя бы в одном из них
                    опустится ниже порога, мы пришлём письмо.</p>
            </div>
        </div>

        <form class="card form" id="product-form" novalidate>
            <div class="form-row">
                <div class="field">
                    <label class="label" for="name">Название товара</label>
                    <div class="input-wrap">
                        <input class="input" id="name" name="name" maxlength="255" required
                               placeholder="" value="${product?.name ?? ''}">
                    </div>
                    <p class="field-error" data-error-for="name"></p>
                </div>
                <div class="field">
                    <label class="label" for="targetPrice">Уведомить, если цена ниже</label>
                    <div class="input-wrap">
                        <input class="input" id="targetPrice" name="targetPrice" inputmode="decimal" required
                               placeholder=""
                               value="${product ? formatPrice(product.targetPrice).replace(' ₽', '') : ''}">
                        <span class="input-suffix">₽</span>
                    </div>
                    <p class="field-error" data-error-for="targetPrice"></p>
                </div>
            </div>

            <div class="field">
                <span class="label">Ссылки на товар</span>
                <div class="links-list" id="links"></div>
                <p class="field-error" data-error-for="links"></p>
                <div>
                    <button class="btn btn-sm" type="button" id="add-link">${icon('plus')} Добавить ссылку</button>
                </div>
            </div>

            <p class="hint">${icon('clock')} Цены проверяются дважды в день — в 9:00 и 21:00 по Москве. История хранится 30 дней.</p>
            <p class="form-error" data-error-for="form"></p>

            <div class="form-actions">
                <a class="btn" href="${backHref}">Отмена</a>
                <button class="btn btn-primary" type="submit">${isEdit ? 'Сохранить' : 'Начать отслеживание'}</button>
            </div>
        </form>
    `);

    const form = view.querySelector('#product-form');
    const list = form.querySelector('#links');
    const addButton = form.querySelector('#add-link');

    /** Метка магазина и подсветка ошибки для одной строки — без перерисовки, чтобы не терять фокус. */
    function updateRow(row, value) {
        const chip = row.querySelector('.mp-chip');
        const wrap = row.querySelector('.input-wrap');
        const error = row.querySelector('.field-error');
        const marketplace = value.trim() ? detectMarketplace(marketplaces, value) : null;

        chip.className = `mp-chip mp-${marketplace ? marketplace.code : 'none'}`;
        chip.textContent = marketplace ? marketplace.name : value.trim() ? '—' : 'Магазин';

        const isBad = value.trim() !== '' && !marketplace;
        wrap.classList.toggle('has-error', isBad);
        error.textContent = isBad ? unsupported : '';
    }

    function drawLinks() {
        mount(list, links.map((url, i) => html`
            <div class="link-row" data-index="${i}">
                <div class="input-wrap">
                    <span class="mp-chip mp-none">Магазин</span>
                    <input class="input" name="link" type="url" inputmode="url" value="${url}"
                           aria-label="Ссылка ${i + 1}" placeholder="https://www.ozon.ru/product/…">
                    <button class="icon-btn" type="button" data-remove="${i}" aria-label="Убрать ссылку"
                            ${links.length === 1 ? html`disabled` : ''}>${icon('close')}</button>
                </div>
                <p class="field-error" data-error-for="links[${i}]"></p>
            </div>`));

        list.querySelectorAll('.link-row').forEach(row => {
            const index = Number(row.dataset.index);
            const input = row.querySelector('input');
            updateRow(row, input.value);
            input.addEventListener('input', () => {
                links[index] = input.value;
                updateRow(row, input.value);
            });
        });
        list.querySelectorAll('[data-remove]').forEach(button => {
            button.addEventListener('click', () => {
                links.splice(Number(button.dataset.remove), 1);
                drawLinks();
            });
        });
        addButton.hidden = links.length >= MAX_LINKS;
    }

    addButton.addEventListener('click', () => {
        links.push('');
        drawLinks();
        list.querySelector('.link-row:last-child input').focus();
    });

    drawLinks();

    form.addEventListener('submit', async event => {
        event.preventDefault();
        // form.name — это имя самой формы, поэтому поле берём по id
        const name = form.querySelector('#name').value.trim();
        const targetPrice = parsePrice(form.querySelector('#targetPrice').value);
        const filled = links.map(l => l.trim()).filter(Boolean);

        const errors = {};
        if (!name) errors.name = 'Введите название товара';
        if (Number.isNaN(targetPrice)) errors.targetPrice = 'Введите цену числом, например 27 000';
        else if (targetPrice <= 0) errors.targetPrice = 'Цена должна быть больше нуля';
        if (filled.length === 0) errors.links = 'Добавьте хотя бы одну ссылку';
        links.forEach((url, i) => {
            if (url.trim() && !detectMarketplace(marketplaces, url)) errors[`links[${i}]`] = unsupported;
        });
        if (Object.keys(errors).length) {
            showFieldErrors(form, errors);
            return;
        }
        clearFieldErrors(form);

        // Пустые строки не отправляем; номера ошибок сервера тогда совпадают со списком filled
        links.splice(0, links.length, ...filled);
        drawLinks();

        const data = { name, targetPrice, links: filled };
        await withBusy(form.querySelector('[type=submit]'), async () => {
            try {
                const saved = isEdit ? await api.updateProduct(productId, data) : await api.createProduct(data);
                toast(isEdit ? 'Изменения сохранены' : 'Товар добавлен');
                ctx.navigate(`product-${saved.id}`);
            } catch (error) {
                if (error instanceof ApiError && Object.keys(error.fieldErrors).length) {
                    showFieldErrors(form, error.fieldErrors);
                } else {
                    showFieldErrors(form, { form: error.message });
                }
            }
        });
    });
}
