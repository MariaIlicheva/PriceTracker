#!/usr/bin/env bash
# Развёртывание PriceTracker: собрать образы, запустить контейнеры и проверить, что сайт отвечает.
# Запуск из корня проекта: ./deploy.sh
set -euo pipefail

cd "$(dirname "$0")"   # работать из папки, где лежит скрипт, откуда бы его ни запустили

if [ ! -f .env ]; then
    echo "Нет файла .env. Создайте его из шаблона и задайте пароль базы:" >&2
    echo "  cp .env.example .env" >&2
    exit 1
fi

echo "==> Сборка образов"
docker compose build

echo "==> Запуск контейнеров (ждём, пока база и бэкенд станут готовы)"
if ! docker compose up -d; then
    echo "Контейнеры не запустились. Последние строки лога бэкенда:" >&2
    docker compose logs --tail 50 backend >&2
    exit 1
fi

echo "==> Проверка: сайт отвечает через nginx"
for attempt in $(seq 1 10); do
    if curl -fsS -o /dev/null http://localhost/api/v1/auth/csrf; then
        echo
        echo "Готово!"
        echo "  Сайт:             http://localhost"
        echo "  Письма (Mailpit): http://localhost:8025"
        exit 0
    fi
    sleep 2
done

echo "Сайт не отвечает. Состояние контейнеров:" >&2
docker compose ps >&2
exit 1