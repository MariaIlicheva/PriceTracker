// Точка входа: узнаём, выполнен ли вход, и показываем нужный экран.
// Экраны переключаются по адресу после «#»: #products, #product-7, #edit-7…
// Страница при этом не перезагружается, поэтому nginx достаточно отдавать один index.html.

import { api, ApiError } from './api.js';
import { html, icon, mount, toast } from './ui.js';
import { renderLogin, renderRegister } from './pages/auth.js';
import { renderProducts } from './pages/products.js';
import { renderProductForm } from './pages/product-form.js';
import { renderProduct } from './pages/product.js';
import { renderProfile } from './pages/profile.js';

const view = document.getElementById('view');
const topbar = document.getElementById('topbar');

/** guest: экран для тех, кто не вошёл (вход и регистрация). */
const ROUTES = [
    { pattern: /^login$/, guest: true, title: 'Вход', render: renderLogin },
    { pattern: /^register$/, guest: true, title: 'Регистрация', render: renderRegister },
    { pattern: /^products$/, title: 'Мои товары', nav: 'products', render: renderProducts },
    { pattern: /^new$/, title: 'Новый товар', nav: 'new', render: (el, ctx) => renderProductForm(el, ctx, null) },
    { pattern: /^product-(\d+)$/, title: 'Товар', render: (el, ctx, m) => renderProduct(el, ctx, Number(m[1])) },
    { pattern: /^edit-(\d+)$/, title: 'Изменение товара', render: (el, ctx, m) => renderProductForm(el, ctx, Number(m[1])) },
    { pattern: /^profile$/, title: 'Профиль', nav: 'profile', render: renderProfile },
];

const state = {
    user: null,
    renderId: 0, // защищает от гонки: ответ старого экрана не перерисует новый
};

const ctx = {
    get user() {
        return state.user;
    },
    setUser(user) {
        state.user = user;
    },
    navigate(name) {
        if (location.hash === `#${name}`) {
            route();
        } else {
            location.hash = name;
        }
    },
    isCurrent(renderId) {
        return renderId === state.renderId;
    },
};

function renderTopbar(active) {
    if (!state.user) {
        topbar.hidden = true;
        return;
    }
    const link = (name, label, extra = '') => html`
        <a href="#${name}" class="${extra}" ${name === active ? html`aria-current="page"` : ''}>${label}</a>`;

    mount(topbar, html`
        <div class="topbar-inner">
            <a class="logo" href="#products">PriceTracker</a>
            <nav class="nav" aria-label="Основное меню">
                ${link('products', 'Мои товары')}
                ${link('new', html`${icon('plus')} Добавить`)}
                ${link('profile', state.user.email, 'nav-user')}
                <button class="btn btn-sm" type="button" id="logout">Выйти</button>
            </nav>
        </div>
    `);
    topbar.hidden = false;
    topbar.querySelector('#logout').addEventListener('click', logout);
}

async function logout() {
    try {
        await api.logout();
    } catch (error) {
        // Даже если сессия уже истекла, на клиенте выходим
    }
    state.user = null;
    ctx.navigate('login');
}

async function route() {
    const name = location.hash.slice(1) || 'products';
    const found = ROUTES.map(r => ({ r, match: name.match(r.pattern) })).find(x => x.match);

    if (!found) {
        ctx.navigate(state.user ? 'products' : 'login');
        return;
    }
    const { r, match } = found;
    if (!r.guest && !state.user) {
        ctx.navigate('login');
        return;
    }
    if (r.guest && state.user) {
        ctx.navigate('products');
        return;
    }

    const renderId = ++state.renderId;
    document.title = `${r.title} · PriceTracker`;
    renderTopbar(r.nav);
    mount(view, html`<p class="loading">Загружаем…</p>`);
    window.scrollTo(0, 0);

    try {
        await r.render(view, { ...ctx, renderId }, match);
        if (ctx.isCurrent(renderId)) view.focus({ preventScroll: true });
    } catch (error) {
        if (!ctx.isCurrent(renderId)) return;
        if (error instanceof ApiError && error.status === 401) {
            state.user = null;
            ctx.navigate('login');
            return;
        }
        console.error(error);
        mount(view, html`
            <div class="card empty">
                <h2>Не получилось открыть страницу</h2>
                <p class="muted">${error.message}</p>
                <a class="btn" href="#products">К моим товарам</a>
            </div>`);
        toast(error.message, 'error');
    }
}

async function start() {
    try {
        state.user = await api.me();
    } catch (error) {
        if (!(error instanceof ApiError && error.status === 401)) {
            toast(error.message, 'error');
        }
        state.user = null;
    }
    window.addEventListener('hashchange', route);
    route();
}

start();
