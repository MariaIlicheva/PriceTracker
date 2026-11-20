# CI/CD и развёртывание

## Путь изменения от коммита до работающего сайта

```mermaid
flowchart LR
    dev["Разработчик<br/>git push"] --> gh["GitHub"]
    gh --> bci["Backend CI<br/>mvn verify<br/>docker build"]
    gh --> fci["Frontend CI<br/>проверка JS и ссылок<br/>docker build, nginx -t"]
    bci --> ok{"всё зелёное?"}
    fci --> ok
    ok -- да --> deploy["Сервер: git pull<br/>./deploy.sh"]
    deploy --> run["docker compose build<br/>docker compose up -d<br/>проверка /api"]
```

- **Backend CI** (`.github/workflows/backend.yml`) запускается при изменениях в `backend/`: сборка и тесты Maven, затем сборка Docker-образа.
- **Frontend CI** (`.github/workflows/frontend.yml`) запускается при изменениях в `frontend/`: проверка синтаксиса JavaScript, ссылок между файлами, сборка образа nginx и проверка его конфигурации.
- **Развёртывание** — скрипт `deploy.sh`: собирает образы, запускает контейнеры, ждёт готовности бэкенда и проверяет, что сайт отвечает. Миграции БД применяет Flyway при старте бэкенда.

## Контейнеры

```mermaid
flowchart LR
    browser["Браузер"] -- ":80" --> nginx["frontend<br/>nginx: статика,<br/>прокси /api"]
    nginx -- "/api → :8080" --> backend["backend<br/>Spring Boot"]
    backend -- ":5432" --> db[("db<br/>PostgreSQL 16<br/>том pgdata")]
    backend -- "SMTP :1025" --> mail["mailpit<br/>тестовая почта, :8025"]
    backend -- "HTTPS" --> shops["Маркетплейсы"]
```

| Сервис | Образ | Наружу | Готовность |
|---|---|---|---|
| `db` | `postgres:16` | 5432 (для разработки) | `pg_isready` |
| `backend` | собирается из `backend/Dockerfile` | — | `GET /actuator/health` |
| `frontend` | собирается из `frontend/Dockerfile` | 80 | — |
| `mailpit` | `axllent/mailpit` | 1025, 8025 | — |

Порядок запуска: `db` (healthy) → `backend` (healthy) → `frontend`.