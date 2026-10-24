-- V2. Секции таблицы price_history: по одной на сутки, хранение 30 дней

-- Границы суток считаются по UTC
-- Замеры в 09:00 и 21:00 МСК
-- Секция за 24.10.2026 называется price_history_p20261024.

-- Создаёт недостающие секции за дни с p_from по p_to включительно.
CREATE FUNCTION create_price_history_partitions(p_from DATE, p_to DATE)
    RETURNS INTEGER
    LANGUAGE plpgsql
SECURITY DEFINER -- функция выполняется с правами того, кто её создал (владельца), а не того, кто её вызывает
SET search_path = public -- обязательная пара к SECURITY DEFINER. Функция ищет таблицы только в схеме public, поэтому вызывающий не может подсунуть ей свою таблицу с таким же именем
AS $$
DECLARE
    d       DATE;
    created INTEGER := 0;
    part    TEXT;
BEGIN
    IF p_from > p_to THEN
        RAISE EXCEPTION 'Начальная дата % позже конечной %', p_from, p_to;
END IF;

    d := p_from;
    WHILE d <= p_to LOOP
        part := 'price_history_p' || to_char(d, 'YYYYMMDD');
        IF to_regclass(part) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF price_history FOR VALUES FROM (%L) TO (%L)',
                part,
                (d::TIMESTAMP AT TIME ZONE 'UTC'),
                ((d + 1)::TIMESTAMP AT TIME ZONE 'UTC')
            );
            created := created + 1;
END IF;
        d := d + 1;
END LOOP;

RETURN created;
END;
$$;

-- Ежедневное обслуживание: создать секции на p_days_ahead дней вперёд и удалить секции старше p_keep_days дней
-- DROP TABLE удаляет сутки данных мгновенно, без медленного DELETE и без раздувания таблицы
CREATE FUNCTION maintain_price_history_partitions(p_days_ahead INTEGER DEFAULT 7,
                                                  p_keep_days  INTEGER DEFAULT 30)
    RETURNS TABLE (partitions_created INTEGER, partitions_dropped INTEGER)
    LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
today  DATE := (now() AT TIME ZONE 'UTC')::DATE;
    cutoff DATE := today - p_keep_days;
    part   RECORD;
BEGIN
    partitions_created := create_price_history_partitions(today, today + p_days_ahead);
    partitions_dropped := 0;

FOR part IN
SELECT c.relname
FROM pg_inherits i
         JOIN pg_class c ON c.oid = i.inhrelid
WHERE i.inhparent = 'price_history'::regclass
          AND c.relname ~ '^price_history_p[0-9]{8}$'
          AND to_date(substring(c.relname FROM '[0-9]{8}$'), 'YYYYMMDD') < cutoff
    LOOP
        EXECUTE format('DROP TABLE %I', part.relname);
partitions_dropped := partitions_dropped + 1;
END LOOP;

    RETURN NEXT;
END;
$$;

-- Начальные секции: 30 дней назад (чтобы можно было загрузить историю для демонстрации)
-- и 7 дней вперёд.
SELECT create_price_history_partitions(
               (now() AT TIME ZONE 'UTC')::DATE - 30,
               (now() AT TIME ZONE 'UTC')::DATE + 7
       );