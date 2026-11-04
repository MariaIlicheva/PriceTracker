-- Тестовая история цен за 30 дней: по два замера в день, в 9:00 и 21:00 по Москве (6:00 и 18:00 UTC).
-- Только для разработки: на шаге 8 цены начнёт собирать парсер.

-- Секции за нужные дни. Функция создаёт только недостающие, поэтому запускать можно сколько угодно раз.
SELECT create_price_history_partitions(
               (now() AT TIME ZONE 'UTC')::date - 30,
               (now() AT TIME ZONE 'UTC')::date + 7
       );

INSERT INTO price_history (link_id, captured_at, price)
SELECT l.id,
       t.captured_at,
       -- Цена плавно идёт от «на 18% выше порога» к своему итоговому уровню, плюс шум ±2%.
       -- Итоговый уровень у ссылок разный (90%, 97%, 104% или 111% порога),
       -- поэтому одни товары окажутся ниже порога, а другие нет.
       round(
               (target.price * (1.18 - (1.18 - (0.90 + (l.id % 4) * 0.07)) * t.progress
                   + (random() - 0.5) * 0.04))::numeric,
               -1) AS price
FROM links l
         -- Порог: самый высокий среди товаров, которые следят за этой ссылкой
         CROSS JOIN LATERAL (
    SELECT max(p.target_price) AS price
    FROM product_links pl
             JOIN products p ON p.id = pl.product_id
    WHERE pl.link_id = l.id
    ) target
         CROSS JOIN LATERAL (
    SELECT s.at                                                    AS captured_at,
           extract(EPOCH FROM s.at - s.first) / extract(EPOCH FROM now() - s.first) AS progress
    FROM (SELECT gs AS at, first
          FROM (SELECT (date_trunc('day', now() AT TIME ZONE 'UTC') - INTERVAL '29 days' + INTERVAL '6 hours')
                           AT TIME ZONE 'UTC' AS first) f,
               generate_series(f.first, now(), INTERVAL '12 hours') gs) s
    ) t
WHERE target.price IS NOT NULL
-- По порядку времени: триггер обновляет текущую цену ссылки и создаёт уведомления
-- только для замеров новее последнего
ORDER BY t.captured_at, l.id
ON CONFLICT (link_id, captured_at) DO NOTHING;