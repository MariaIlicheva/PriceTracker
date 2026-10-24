-- V1. Таблицы, ключи, ограничения целостности и индексы

-- Пользователи ---------------------------------------------------------------
CREATE TABLE users (
                       id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT users_email_format_chk CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
    );

-- Почта уникальна без учёта регистра
CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));

-- Справочник маркетплейсов ---------------------------------------------------
CREATE TABLE marketplaces (
                              id   SMALLINT     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              code VARCHAR(20)  NOT NULL,
                              name VARCHAR(100) NOT NULL,
                              CONSTRAINT marketplaces_code_uq  UNIQUE (code),
                              CONSTRAINT marketplaces_name_uq  UNIQUE (name),
                              CONSTRAINT marketplaces_code_chk CHECK (code IN ('wb', 'ozon', 'ym', 'kuper', 'ali'))
);

-- Домены маркетплейсов: у одного маркетплейса может быть несколько доменов
CREATE TABLE marketplace_domains (
                                     domain         VARCHAR(100) PRIMARY KEY,
                                     marketplace_id SMALLINT     NOT NULL REFERENCES marketplaces (id) ON DELETE CASCADE,
                                     CONSTRAINT marketplace_domains_lower_chk CHECK (domain = lower(domain))
    );

CREATE INDEX marketplace_domains_marketplace_idx ON marketplace_domains (marketplace_id);

-- Товары пользователей -------------------------------------------------------
CREATE TABLE products (
                          id           BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                          name         VARCHAR(255)  NOT NULL,
                          target_price NUMERIC(12,2) NOT NULL,
                          status       VARCHAR(10)   NOT NULL DEFAULT 'active',
                          created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          CONSTRAINT products_user_name_uq     UNIQUE (user_id, name),
                          CONSTRAINT products_name_chk         CHECK (length(trim(name)) > 0),
                          CONSTRAINT products_target_price_chk CHECK (target_price > 0),
                          CONSTRAINT products_status_chk       CHECK (status IN ('active', 'paused'))
);

-- Ссылки на страницы товаров (общие для всех пользователей) ------------------
CREATE TABLE links (
                       id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       marketplace_id  SMALLINT      NOT NULL REFERENCES marketplaces (id) ON DELETE RESTRICT,
                       url             TEXT          NOT NULL,
                       last_price      NUMERIC(12,2),
                       last_checked_at TIMESTAMPTZ,
                       CONSTRAINT links_url_uq         UNIQUE (url),
                       CONSTRAINT links_url_chk        CHECK (url LIKE 'https://%' AND length(url) <= 2048),
                       CONSTRAINT links_last_price_chk CHECK (last_price >= 0)
);

CREATE INDEX links_marketplace_idx ON links (marketplace_id);

-- Связь «товар — ссылка» (многие ко многим) ----------------------------------
CREATE TABLE product_links (
                               product_id BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
                               link_id    BIGINT      NOT NULL REFERENCES links (id) ON DELETE CASCADE,
                               added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                               PRIMARY KEY (product_id, link_id)
);

-- Все товары, подписанные на ссылку: нужен триггеру порога и каскадному удалению ссылок
CREATE INDEX product_links_link_idx ON product_links (link_id);

-- История цен: временной ряд, секционирован по дням (секции создаёт V2) -------
CREATE TABLE price_history (
                               link_id     BIGINT        NOT NULL REFERENCES links (id) ON DELETE CASCADE,
                               captured_at TIMESTAMPTZ   NOT NULL,
                               price       NUMERIC(12,2) NOT NULL,
                               CONSTRAINT price_history_price_chk CHECK (price >= 0),
                               PRIMARY KEY (link_id, captured_at)
) PARTITION BY RANGE (captured_at);

-- Очередь задач парсинга -----------------------------------------------------
CREATE TABLE parse_jobs (
                            id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            link_id      BIGINT      NOT NULL REFERENCES links (id) ON DELETE CASCADE,
                            scheduled_at TIMESTAMPTZ NOT NULL,
                            status       VARCHAR(12) NOT NULL DEFAULT 'pending',
                            attempts     SMALLINT    NOT NULL,
                            locked_at    TIMESTAMPTZ,
                            error        TEXT,
                            CONSTRAINT parse_jobs_link_scheduled_uq UNIQUE (link_id, scheduled_at),
                            CONSTRAINT parse_jobs_status_chk   CHECK (status IN ('pending', 'processing', 'done', 'failed')),
                            CONSTRAINT parse_jobs_attempts_chk CHECK (attempts BETWEEN 0 AND 3),
                            CONSTRAINT parse_jobs_locked_chk   CHECK (status <> 'processing' OR locked_at IS NOT NULL)
);

-- Частичный индекс: планировщик ищет только ожидающие задачи, выполненные в индекс не попадают.
CREATE INDEX parse_jobs_pending_idx ON parse_jobs (scheduled_at) WHERE status = 'pending';

-- Очередь уведомлений --------------------------------------------------------
CREATE TABLE notifications (
                               id         BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               product_id BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
                               link_id    BIGINT        NOT NULL REFERENCES links (id) ON DELETE CASCADE,
                               price      NUMERIC(12,2) NOT NULL,
                               status     VARCHAR(10)   NOT NULL DEFAULT 'pending',
                               created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
                               sent_at    TIMESTAMPTZ,
                               CONSTRAINT notifications_price_chk  CHECK (price >= 0),
                               CONSTRAINT notifications_status_chk CHECK (status IN ('pending', 'sent', 'failed')),
                               CONSTRAINT notifications_sent_chk   CHECK ((status = 'sent') = (sent_at IS NOT NULL))
);

CREATE INDEX notifications_pending_idx ON notifications (created_at) WHERE status = 'pending';
CREATE INDEX notifications_product_idx ON notifications (product_id);
CREATE INDEX notifications_link_idx    ON notifications (link_id);