// Обращения к REST API бэкенда. Формат запросов и ответов описан в docs/api.md.
// Фронтенд и API открываются с одного адреса (nginx пересылает /api на бэкенд),
// поэтому cookie сессии JSESSIONID браузер отправляет сам.

const BASE = '/api/v1';
const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

/** Ошибка от сервера: код ответа, общий текст и ошибки по полям формы. */
export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || defaultMessage(status));
        this.status = status;
        this.fieldErrors = body?.fieldErrors || {};
    }
}

function defaultMessage(status) {
    if (status === 401) return 'Войдите, чтобы продолжить';
    if (status === 403) return 'Действие запрещено. Обновите страницу и попробуйте ещё раз';
    if (status === 404) return 'Не найдено';
    if (status >= 500) return 'Сервер не отвечает. Попробуйте позже';
    return 'Что-то пошло не так';
}

function readCookie(name) {
    const prefix = `${name}=`;
    const found = document.cookie.split('; ').find(part => part.startsWith(prefix));
    return found ? decodeURIComponent(found.slice(prefix.length)) : null;
}

/**
 * CSRF-токен для POST/PUT/PATCH/DELETE. Обычно он уже лежит в cookie XSRF-TOKEN.
 * После входа Spring удаляет старый токен, тогда берём новый у /auth/csrf.
 */
async function csrfToken(forceFresh) {
    if (!forceFresh) {
        const fromCookie = readCookie(CSRF_COOKIE);
        if (fromCookie) return fromCookie;
    }
    const response = await fetch(`${BASE}/auth/csrf`, { credentials: 'same-origin' });
    if (!response.ok) throw new ApiError(response.status, null);
    const data = await response.json();
    return data.token;
}

async function readBody(response) {
    const type = response.headers.get('Content-Type') || '';
    if (!type.includes('application/json')) return null;
    try {
        return await response.json();
    } catch {
        return null;
    }
}

/**
 * @param {string} method HTTP-метод
 * @param {string} path путь после /api/v1
 * @param {{json?: object, form?: object}} payload тело в JSON или как HTML-форма (для входа)
 */
async function request(method, path, payload = {}, isRetry = false) {
    const headers = { Accept: 'application/json' };
    let body;

    if (payload.json !== undefined) {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(payload.json);
    } else if (payload.form !== undefined) {
        body = new URLSearchParams(payload.form); // браузер сам поставит x-www-form-urlencoded
    }
    if (method !== 'GET') {
        headers[CSRF_HEADER] = await csrfToken(isRetry);
    }

    let response;
    try {
        response = await fetch(BASE + path, { method, headers, body, credentials: 'same-origin' });
    } catch {
        throw new ApiError(0, { message: 'Нет связи с сервером. Проверьте интернет' });
    }

    // Токен устарел (например, сессия сменилась в другой вкладке): один раз берём новый и повторяем
    if (response.status === 403 && method !== 'GET' && !isRetry) {
        return request(method, path, payload, true);
    }
    if (response.status === 204) return null;

    const data = await readBody(response);
    if (!response.ok) throw new ApiError(response.status, data);
    return data;
}

export const api = {
    me: () => request('GET', '/me'),
    register: (email, password) => request('POST', '/auth/register', { json: { email, password } }),
    login: (email, password) => request('POST', '/auth/login', { form: { email, password } }),
    logout: () => request('POST', '/auth/logout'),
    deleteAccount: () => request('DELETE', '/me'),

    marketplaces: () => request('GET', '/marketplaces'),

    products: () => request('GET', '/products'),
    product: id => request('GET', `/products/${id}`),
    createProduct: data => request('POST', '/products', { json: data }),
    updateProduct: (id, data) => request('PUT', `/products/${id}`, { json: data }),
    setStatus: (id, status) => request('PATCH', `/products/${id}/status`, { json: { status } }),
    deleteProduct: id => request('DELETE', `/products/${id}`),
    history: (id, period) => request('GET', `/products/${id}/history?period=${encodeURIComponent(period)}`),
};
