// Профиль: почта и удаление аккаунта с подтверждением прямо на странице.

import { api } from '../api.js';
import { html, mount, toast, withBusy } from '../ui.js';

export function renderProfile(view, ctx) {
    mount(view, html`
        <div class="page-head">
            <div>
                <h1>Профиль</h1>
            </div>
        </div>

        <div class="stack">
            <section class="card">
                <p class="muted">Письма о снижении цены приходят на</p>
                <p class="profile-email">${ctx.user.email}</p>
            </section>

            <section class="card">
                <div class="card-head">
                    <div>
                        <h2>Удаление профиля</h2>
                        <p class="muted">Вместе с профилем удалятся все товары и их история цен. Восстановить их не получится.</p>
                    </div>
                    <button class="btn btn-soft-danger" type="button" id="ask-delete">Удалить профиль</button>
                </div>
                <div class="confirm" id="confirm-delete" hidden>
                    <p>Точно удалить профиль ${ctx.user.email}?</p>
                    <div class="actions">
                        <button class="btn btn-danger" type="button" id="do-delete">Удалить навсегда</button>
                        <button class="btn" type="button" id="cancel-delete">Отмена</button>
                    </div>
                </div>
            </section>
        </div>
    `);

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
                await api.deleteAccount();
                ctx.setUser(null);
                toast('Профиль удалён');
                ctx.navigate('login');
            } catch (error) {
                toast(error.message, 'error');
            }
        });
    });
}
