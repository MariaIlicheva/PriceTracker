// Общие помощники интерфейса: безопасная вставка HTML, форматирование цен и дат,
// ошибки полей формы, всплывающие сообщения.

// ---------- HTML ----------

/** Уже готовый HTML, который не нужно экранировать. */
class SafeHtml {
    constructor(value) {
        this.value = value;
    }

    toString() {
        return this.value;
    }
}

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

export function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, ch => ESCAPES[ch]);
}

function toHtml(value) {
    if (value === null || value === undefined || value === false) return '';
    if (value instanceof SafeHtml) return value.value;
    if (Array.isArray(value)) return value.map(toHtml).join('');
    return escapeHtml(value);
}

/**
 * Шаблон html`...`: всё, что подставлено через ${...}, экранируется.
 * Поэтому название товара вроде "<script>" покажется текстом, а не выполнится.
 * Вложенные html`...` и массивы из них вставляются как есть.
 */
export function html(strings, ...values) {
    let out = '';
    strings.forEach((str, i) => {
        out += str;
        if (i < values.length) out += toHtml(values[i]);
    });
    return new SafeHtml(out);
}

/** Вставляет готовый HTML внутрь элемента. */
export function mount(element, content) {
    element.innerHTML = toHtml(content);
}

/** Иконка из набора ниже. */
export function icon(name) {
    return new SafeHtml(`<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${ICONS[name]}</svg>`);
}

const ICONS = {
    plus: '<path d="M12 5v14M5 12h14"/>',
    close: '<path d="M6 6l12 12M18 6L6 18"/>',
    back: '<path d="M15 6l-6 6 6 6"/>',
    pause: '<path d="M9 6v12M15 6v12"/>',
    play: '<path d="M8 5.5v13l10.5-6.5z"/>',
    edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/>',
    trash: '<path d="M5 7h14M10 7V4.5h4V7M7 7l1 13h8l1-13"/>',
    eye: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
    link: '<path d="M10 14a4 4 0 005.7 0l3-3a4 4 0 00-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 00-5.7 0l-3 3a4 4 0 005.7 5.7l1-1"/>',
    chart: '<path d="M4 18l5-6 4 3 7-9"/>',
    mail: '<rect x="3" y="5.5" width="18" height="13" rx="3"/><path d="M4 7.5l8 6 8-6"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    external: '<path d="M14 5h5v5M19 5l-8 8M18 14v4a1 1 0 01-1 1H6a1 1 0 01-1-1V7a1 1 0 011-1h4"/>',
};

// ---------- Цены и даты ----------

const priceFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 });

/** 26500 → «26 500 ₽» */
export function formatPrice(value) {
    if (value === null || value === undefined) return '—';
    return `${priceFormat.format(Number(value))} ₽`;
}

/** «27 000», «27000,50» → 27000.5; пустая или неверная строка → NaN */
export function parsePrice(text) {
    const cleaned = String(text).replace(/[\s\u00a0\u202f₽]/g, '').replace(',', '.');
    if (cleaned === '' || !/^\d+(\.\d{1,2})?$/.test(cleaned)) return NaN;
    return Number(cleaned);
}

// Все даты показываем по Москве: в это время работает сбор цен
const TIME_ZONE = 'Europe/Moscow';
const dayKey = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' });
const timeFormat = new Intl.DateTimeFormat('ru-RU', { timeZone: TIME_ZONE, hour: 'numeric', minute: '2-digit' });
const dateFormat = new Intl.DateTimeFormat('ru-RU', { timeZone: TIME_ZONE, day: 'numeric', month: 'short' });

export function formatShortDate(date) {
    return dateFormat.format(date).replace('.', '');
}

/** «сегодня в 9:00», «вчера в 21:00», «24 окт в 9:00» */
export function formatWhen(iso) {
    if (!iso) return 'ещё не проверялась';
    const date = new Date(iso);
    const today = dayKey.format(new Date());
    const yesterday = dayKey.format(new Date(Date.now() - 24 * 3600 * 1000));
    const day = dayKey.format(date);
    const time = timeFormat.format(date);
    if (day === today) return `сегодня в ${time}`;
    if (day === yesterday) return `вчера в ${time}`;
    return `${formatShortDate(date)} в ${time}`;
}

/**
 * Когда следующая проверка цен: сбор идёт в 9:00 и 21:00 по Москве.
 * Москва живёт в UTC+3 без перехода на летнее время, поэтому сдвиг постоянный.
 */
export function nextCheckText(now = new Date()) {
    const moscow = new Date(now.getTime() + 3 * 3600 * 1000);
    const minutes = moscow.getUTCHours() * 60 + moscow.getUTCMinutes();
    if (minutes < 9 * 60) return 'сегодня в 9:00';
    if (minutes < 21 * 60) return 'сегодня в 21:00';
    return 'завтра в 9:00';
}

/** Склонение: plural(3, 'ссылка', 'ссылки', 'ссылок') → «ссылки» */
export function plural(n, one, few, many) {
    const mod10 = n % 10;
    const mod100 = n % 100;
    if (mod10 === 1 && mod100 !== 11) return one;
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
    return many;
}

export const STATE_LABELS = {
    below: 'Ниже порога',
    tracking: 'Отслеживается',
    paused: 'На паузе',
};

export function statePill(state) {
    return html`<span class="pill pill-${state}">${STATE_LABELS[state]}</span>`;
}

// ---------- Формы ----------

/**
 * Показывает ошибки под полями. Ключи — имена полей из ответа сервера:
 * «email», «name», «links[0]». Под каждым полем есть <p data-error-for="...">.
 */
export function showFieldErrors(form, fieldErrors) {
    clearFieldErrors(form);
    for (const [field, message] of Object.entries(fieldErrors)) {
        const slot = form.querySelector(`[data-error-for="${CSS.escape(field)}"]`);
        if (!slot) continue;
        slot.textContent = message;
        slot.closest('.field, .link-row')?.querySelector('.input-wrap')?.classList.add('has-error');
    }
}

export function clearFieldErrors(form) {
    form.querySelectorAll('[data-error-for]').forEach(slot => {
        slot.textContent = '';
    });
    form.querySelectorAll('.has-error').forEach(wrap => wrap.classList.remove('has-error'));
}

/** Блокирует кнопку на время запроса, чтобы форму не отправили дважды. */
export async function withBusy(button, action) {
    button.disabled = true;
    try {
        return await action();
    } finally {
        button.disabled = false;
    }
}

// ---------- Всплывающие сообщения ----------

export function toast(message, kind = 'info') {
    const box = document.getElementById('toasts');
    const item = document.createElement('div');
    item.className = kind === 'error' ? 'toast toast-error' : 'toast';
    item.textContent = message;
    box.append(item);
    setTimeout(() => item.remove(), 3500);
}
