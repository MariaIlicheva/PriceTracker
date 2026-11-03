// Справочник магазинов: загружается с сервера один раз и нужен, чтобы
// сразу при вводе ссылки показать метку магазина и подсветить неподдерживаемые сайты.
// Окончательно ссылку всё равно проверяет сервер.

import { api } from './api.js';

let cache = null;

/** @returns {Promise<Array<{code: string, name: string, domains: string[]}>>} */
export function loadMarketplaces() {
    if (!cache) {
        cache = api.marketplaces().catch(error => {
            cache = null; // при ошибке попробуем снова в следующий раз
            throw error;
        });
    }
    return cache;
}

/** Хост ссылки так же, как его берёт сервер: без схемы, «www.» и в нижнем регистре. */
export function hostOf(rawUrl) {
    let text = String(rawUrl).trim();
    if (!text) return null;
    if (!text.includes('://')) text = `https://${text}`;
    try {
        const url = new URL(text);
        if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;
        return url.hostname.toLowerCase().replace(/^www\./, '');
    } catch {
        return null;
    }
}

/** Магазин по ссылке или null, если сайт не поддерживается. */
export function detectMarketplace(marketplaces, rawUrl) {
    const host = hostOf(rawUrl);
    if (!host) return null;
    return marketplaces.find(m => m.domains.some(d => host === d || host.endsWith(`.${d}`))) || null;
}

/** Код магазина по названию — в списке товаров сервер отдаёт только название. */
export function codeByName(marketplaces, name) {
    return marketplaces.find(m => m.name === name)?.code || 'none';
}

export function supportedNames(marketplaces) {
    return marketplaces.map(m => m.name).join(', ');
}
