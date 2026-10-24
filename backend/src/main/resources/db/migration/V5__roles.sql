-- V5. Разграничение прав доступа к базе данных
--
--   price_owner — владелец таблиц и функций. От его имени выполняются миграции
--   price_app   — роль приложения: только чтение и изменение данных (DML),
--                 без права менять структуру базы (DDL).
--
-- Обе роли созданы без права входа, это «групповые» роли с набором прав
-- Роли существуют на уровне всего сервера PostgreSQL, поэтому создаются, только если их ещё нет

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'price_owner') THEN
CREATE ROLE price_owner NOLOGIN;
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'price_app') THEN
CREATE ROLE price_app NOLOGIN;
END IF;
END;
$$;

-- price_owner создаёт новые секции истории цен, для этого ему нужно право CREATE в схеме
GRANT USAGE, CREATE ON SCHEMA public TO price_owner;

-- Владельцем всех таблиц и функций становится price_owner.
DO $$
DECLARE
    obj RECORD;
BEGIN
FOR obj IN
SELECT c.relname
FROM pg_class c
         JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind IN ('r', 'p')
  AND c.relname <> 'flyway_schema_history'
    LOOP
        EXECUTE format('ALTER TABLE %I OWNER TO price_owner', obj.relname);
END LOOP;
END;
$$;

ALTER FUNCTION create_price_history_partitions(DATE, DATE)           OWNER TO price_owner;
ALTER FUNCTION maintain_price_history_partitions(INTEGER, INTEGER)   OWNER TO price_owner;
ALTER FUNCTION price_history_after_insert()                          OWNER TO price_owner;
ALTER FUNCTION check_product_link_count()                            OWNER TO price_owner;
ALTER FUNCTION delete_unused_link()                                  OWNER TO price_owner;

-- Права приложения
GRANT USAGE ON SCHEMA public TO price_app;

GRANT SELECT, INSERT, UPDATE, DELETE
      ON users, products, links, product_links, price_history, parse_jobs, notifications
          TO price_app;

-- Справочники приложение только читает: менять их можно лишь миграциями
GRANT SELECT ON marketplaces, marketplace_domains TO price_app;

-- Функции обслуживания секций объявлены SECURITY DEFINER: они выполняются с правами price_owner,
-- поэтому приложение может вызывать их раз в сутки, не имея права создавать и удалять таблицы само.
REVOKE EXECUTE ON FUNCTION create_price_history_partitions(DATE, DATE)         FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION maintain_price_history_partitions(INTEGER, INTEGER) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION maintain_price_history_partitions(INTEGER, INTEGER) TO price_app;

ALTER DEFAULT PRIVILEGES FOR ROLE price_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO price_app;