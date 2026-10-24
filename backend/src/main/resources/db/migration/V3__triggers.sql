-- V3. Бизнес-логика на стороне базы данных: триггеры

-- Новый замер цены ---------------------------------------------------------
-- После вставки в price_history:
--   создаёт уведомления для активных товаров, у которых цена ссылки только что опустилась ниже порога
--   *бновляет у ссылки последнюю цену и время проверки
-- Всё выполняется в той же транзакции, что и вставка замера,
-- поэтому замер, текущая цена и уведомления фиксируются вместе или не фиксируются вовсе

CREATE FUNCTION price_history_after_insert()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    v_prev_price   NUMERIC(12,2);
    v_prev_checked TIMESTAMPTZ;
BEGIN
    IF NEW.captured_at > now() + INTERVAL '5 minutes' THEN
        RAISE EXCEPTION 'Время замера % в будущем', NEW.captured_at
            USING ERRCODE = 'check_violation';
END IF;

-- Два одновременных замера одной ссылки обработаются по очереди и не создадут дублей уведомлений
SELECT last_price, last_checked_at
INTO v_prev_price, v_prev_checked
FROM links
WHERE id = NEW.link_id
    FOR UPDATE;

-- Замер старше последнего в историю попадает, но текущую цену и уведомления не трогает
IF v_prev_checked IS NOT NULL AND NEW.captured_at < v_prev_checked THEN
        RETURN NULL;
END IF;

INSERT INTO notifications (product_id, link_id, price)
SELECT p.id, NEW.link_id, NEW.price
FROM product_links pl
         JOIN products p ON p.id = pl.product_id
WHERE pl.link_id = NEW.link_id
  AND p.status = 'active'
  AND NEW.price < p.target_price
  AND (v_prev_price IS NULL OR v_prev_price >= p.target_price);

UPDATE links
SET last_price      = NEW.price,
    last_checked_at = NEW.captured_at
WHERE id = NEW.link_id;

RETURN NULL;
END;
$$;

-- Триггер на секционированной таблице автоматически действует во всех её секциях,
-- в том числе созданных позже
CREATE TRIGGER price_history_after_insert
    AFTER INSERT ON price_history
    FOR EACH ROW EXECUTE FUNCTION price_history_after_insert();


-- У товара от 1 до 10 ссылок ----------------------------------------------
-- Проверка отложена до фиксации транзакции: приложение сначала создаёт товар,
-- потом его ссылки, и проверяется уже итоговое состояние.
CREATE FUNCTION check_product_link_count()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
v_product_id BIGINT;
    v_count      INTEGER;
BEGIN
    IF TG_TABLE_NAME = 'products' THEN
        v_product_id := NEW.id;
    ELSIF TG_OP = 'DELETE' THEN
        v_product_id := OLD.product_id;
ELSE
        v_product_id := NEW.product_id;
END IF;

    -- Товар удалён целиком (каскадом вместе со ссылками) — проверять нечего.
    IF NOT EXISTS (SELECT 1 FROM products WHERE id = v_product_id) THEN
        RETURN NULL;
END IF;

SELECT count(*) INTO v_count FROM product_links WHERE product_id = v_product_id;

IF v_count < 1 OR v_count > 10 THEN
        RAISE EXCEPTION 'У товара % должно быть от 1 до 10 ссылок, сейчас %', v_product_id, v_count
            USING ERRCODE = 'check_violation';
END IF;

RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER products_link_count_chk
    AFTER INSERT ON products
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_product_link_count();

CREATE CONSTRAINT TRIGGER product_links_link_count_chk
    AFTER INSERT OR DELETE ON product_links
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_product_link_count();


-- Удаление ссылок, за которыми больше никто не следит ---------------------
-- Когда товар удаляется и на ссылку больше не подписан ни один товар, ссылка удаляется
-- Вместе с ней каскадно удаляются её история цен, задачи парсинга и уведомления.
-- Если ссылку отслеживает кто-то ещё, история остаётся.
CREATE FUNCTION delete_unused_link()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
DELETE FROM links l
WHERE l.id = OLD.link_id
  AND NOT EXISTS (SELECT 1 FROM product_links pl WHERE pl.link_id = OLD.link_id);
RETURN NULL;
END;
$$;

CREATE TRIGGER product_links_delete_unused_link
    AFTER DELETE ON product_links
    FOR EACH ROW EXECUTE FUNCTION delete_unused_link();