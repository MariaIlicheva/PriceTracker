// Экраны «Вход» и «Регистрация». Слева — рассказ о сервисе с пузырями, справа — форма.

import { api, ApiError } from '../api.js';
import { clearFieldErrors, html, icon, mount, showFieldErrors, toast, withBusy } from '../ui.js';

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

function hero() {
    return html`
        <section class="auth-hero">
            <h1>Покупай, когда цена <em>правда</em> упала</h1>
            <ul class="auth-points">
                <li><span class="bubble-icon">${icon('link')}</span>Ссылки на один товар с Wildberries, Ozon, Яндекс Маркета, Купера и AliExpress</li>
                <li><span class="bubble-icon">${icon('chart')}</span>История цены за неделю или месяц на одном графике</li>
                <li><span class="bubble-icon">${icon('mail')}</span>Письмо, как только цена опустится ниже твоего порога</li>
            </ul>
            <div class="scene" aria-hidden="true">
                <span class="bubble scene-bubble-mid"></span>
                <span class="bubble scene-bubble-small"></span>
                <div class="scene-card">
                    <small>Кроссовки · Ozon</small>
                    <span class="price">11 490 ₽</span>
                </div>
                <div class="scene-card">
                    <small>Наушники · Wildberries</small>
                    <span class="old">29 990 ₽</span>
                    <span class="price good">26 500 ₽</span>
                </div>
                <span class="bubble scene-bubble-big"></span>
            </div>
        </section>`;
}

function passwordField(id, label, autocomplete) {
    return html`
        <div class="field">
            <label class="label" for="${id}">${label}</label>
            <div class="input-wrap">
                <input class="input" id="${id}" name="${id}" type="password" autocomplete="${autocomplete}" required>
                <button class="icon-btn" type="button" data-toggle-password="${id}" aria-label="Показать пароль">${icon('eye')}</button>
            </div>
            <p class="field-error" data-error-for="${id}"></p>
        </div>`;
}

function emailField() {
    return html`
        <div class="field">
            <label class="label" for="email">Электронная почта</label>
            <div class="input-wrap">
                <input class="input" id="email" name="email" type="email" autocomplete="email"
                       placeholder="" required>
            </div>
            <p class="field-error" data-error-for="email"></p>
        </div>`;
}

function bindPasswordToggles(root) {
    root.querySelectorAll('[data-toggle-password]').forEach(button => {
        button.addEventListener('click', () => {
            const input = root.querySelector(`#${button.dataset.togglePassword}`);
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.setAttribute('aria-label', show ? 'Скрыть пароль' : 'Показать пароль');
        });
    });
}

export function renderLogin(view, ctx) {
    mount(view, html`
        <div class="auth">
            ${hero()}
            <section class="card auth-card">
                <h2>С возвращением</h2>
                <p class="auth-switch">Нет аккаунта? <a href="#register">Зарегистрироваться</a></p>
                <form class="form" id="login-form" novalidate>
                    ${emailField()}
                    ${passwordField('password', 'Пароль', 'current-password')}
                    <p class="form-error" data-error-for="form"></p>
                    <button class="btn btn-primary btn-lg btn-block" type="submit">Войти</button>
                </form>
            </section>
        </div>`);

    const form = view.querySelector('#login-form');
    bindPasswordToggles(form);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const email = form.email.value.trim();
        const password = form.password.value;
        const errors = {};
        if (!email) errors.email = 'Введите почту';
        if (!password) errors.password = 'Введите пароль';
        if (Object.keys(errors).length) {
            showFieldErrors(form, errors);
            return;
        }
        clearFieldErrors(form);

        await withBusy(form.querySelector('[type=submit]'), async () => {
            try {
                ctx.setUser(await api.login(email, password));
                ctx.navigate('products');
            } catch (error) {
                // Одно общее сообщение: не подсказываем, что именно неверно
                showFieldErrors(form, { form: error.message });
            }
        });
    });
}

export function renderRegister(view, ctx) {
    mount(view, html`
        <div class="auth">
            ${hero()}
            <section class="card auth-card">
                <h2>Создай аккаунт</h2>
                <p class="auth-switch">Уже есть аккаунт? <a href="#login">Войти</a></p>
                <form class="form" id="register-form" novalidate>
                    ${emailField()}
                    ${passwordField('password', 'Пароль', 'new-password')}
                    ${passwordField('repeat', 'Повтори пароль', 'new-password')}
                    <div class="field">
                        <label class="checkbox">
                            <input type="checkbox" id="consent" name="consent">
                            <span>Хочу получать письма о снижении цены</span>
                        </label>
                        <p class="field-error" data-error-for="consent"></p>
                    </div>
                    <p class="form-error" data-error-for="form"></p>
                    <button class="btn btn-primary btn-lg btn-block" type="submit">Создать аккаунт</button>
                </form>
            </section>
        </div>`);

    const form = view.querySelector('#register-form');
    bindPasswordToggles(form);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const email = form.email.value.trim();
        const password = form.password.value;

        // Те же правила, что и на сервере (RegisterRequest), чтобы ошибка была видна сразу
        const errors = {};
        if (!EMAIL_PATTERN.test(email)) errors.email = 'Неверный формат почты';
        if (password.length < 8 || password.length > 72) {
            errors.password = 'Пароль должен быть от 8 до 72 символов';
        } else if (!/\p{L}/u.test(password) || !/\d/.test(password)) {
            errors.password = 'Пароль должен содержать буквы и цифры';
        }
        if (form.repeat.value !== password) errors.repeat = 'Пароли не совпадают';
        if (!form.consent.checked) errors.consent = 'Без писем мы не сможем сообщить о снижении цены';
        if (Object.keys(errors).length) {
            showFieldErrors(form, errors);
            return;
        }
        clearFieldErrors(form);

        await withBusy(form.querySelector('[type=submit]'), async () => {
            try {
                await api.register(email, password);
                // Сервер после регистрации не входит сам — входим сразу же
                ctx.setUser(await api.login(email, password));
                toast('Аккаунт создан');
                ctx.navigate('products');
            } catch (error) {
                if (error instanceof ApiError && Object.keys(error.fieldErrors).length) {
                    showFieldErrors(form, error.fieldErrors);
                    if (error.status === 409) form.querySelector('[data-error-for="email"]').textContent = error.message;
                } else {
                    showFieldErrors(form, { form: error.message });
                }
            }
        });
    });
}
