# PriceTracker

![Backend CI](https://github.com/MariaIlicheva/PriceTracker/actions/workflows/backend.yml/badge.svg)
![Frontend CI](https://github.com/MariaIlicheva/PriceTracker/actions/workflows/frontend.yml/badge.svg)

Удобное веб-приложение для мониторинга цен на товары маркетплейсов. Пользователь добавляет товар и ссылки на его страницы в разных магазинах, приложение собирает цены, показывает их историю на user-friendly графике и присылает письмо, оповещающее о снижении цены ниже заданного порога.


## Возможности

- регистрация и вход по email и паролю;
- добавление товара с несколькими ссылками на Wildberries, Ozon, Яндекс Маркет, Купер и AliExpress;
- автоматический сбор цен в 09:00 и 21:00 МСК;
- график истории цен по каждой ссылке за неделю или месяц, история хранится 30 дней;
- письмо, когда цена опускается ниже порога;
- пауза отслеживания, изменение и удаление товара, удаление профиля.

Чем проект интересен с точки зрения баз данных: очередь задач парсинга в PostgreSQL с `SKIP LOCKED`, триггер проверки порога, секционирование истории цен по дням, общие ссылки для разных пользователей, каскадное удаление и разграничение прав доступа к БД.

## Технологии

| Часть | Что используется |
|---|---|
| Бэкенд | Java 21, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Mail), Hibernate, Maven |
| База данных | PostgreSQL 16, миграции Flyway |
| Фронтенд | HTML, CSS, JavaScript |
| Веб-сервер | nginx: раздаёт фронтенд и проксирует `/api` на бэкенд |
| Парсинг | `java.net.http.HttpClient`, Jackson (разметка schema.org) |
| Инфраструктура | Docker, Docker Compose, GitHub Actions |

## Документация

- [Техническое задание](docs/tz_price_monitoring.xml) (XML) и [его версия для чтения](docs/ТЗ_мониторинг_цен.docx)
- [Архитектура](docs/architecture.md)
- [Пользовательские пути](docs/user-flows.md)
- [Макеты экранов](docs/МАКЕТ.pdf)
- [Описание REST API](docs/api.md)
- [CI/CD и развёртывание](docs/ci-cd.md)

## Структура репозитория

```
backend/              Spring Boot приложение: REST API, сбор цен, миграции БД
frontend/             клиентская часть: HTML, CSS, JavaScript; nginx.conf
docs/                 ТЗ, макеты, архитектура, пользовательские пути, описание API
scripts/dev/          скрипты для разработки (тестовая история цен)
.github/workflows/    CI
docker-compose.yaml   PostgreSQL и nginx для локального запуска
```

## Запуск

```bash
cp .env.example .env     # при желании поменять пароль базы
./deploy.sh
```

Скрипт соберёт образы, запустит базу, бэкенд, nginx и тестовый почтовый сервер и проверит, что сайт отвечает.

- сайт: http://localhost
- письма (Mailpit): http://localhost:8025

Остановить: `docker compose down` (данные базы сохраняются в томе `pgdata`).

### Режим разработки

Бэкенд запускается из IntelliJ IDEA (класс `ru.pet.project.PriceTracker`), а база, почта и nginx — в Docker:

```bash
docker compose -f docker-compose.yaml -f docker-compose.dev.yaml up -d db mailpit frontend
```

nginx при этом отдаёт фронтенд прямо из папки `frontend/` и пересылает `/api` на бэкенд в IDEA.

Тестовая история цен для графика:
```bash
docker exec -i pricetracker-db psql -U pricetracker -d pricetracker < scripts/dev/seed-price-history.sql
```
