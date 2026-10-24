-- V4. Справочник поддерживаемых маркетплейсов и их доменов

-- Домены хранятся без «www.» и без поддоменов. Приложение берёт хост из ссылки
-- и ищет совпадение с доменом либо с его окончанием

INSERT INTO marketplaces (code, name) VALUES
                                          ('wb',    'Wildberries'),
                                          ('ozon',  'Ozon'),
                                          ('ym',    'Яндекс Маркет'),
                                          ('kuper', 'Купер'),
                                          ('ali',   'AliExpress');

INSERT INTO marketplace_domains (domain, marketplace_id)
SELECT d.domain, m.id
FROM (VALUES
          ('wildberries.ru',   'wb'),
          ('ozon.ru',          'ozon'),
          ('market.yandex.ru', 'ym'),
          ('kuper.ru',         'kuper'),
          ('aliexpress.ru',    'ali'),
          ('aliexpress.com',   'ali')
     ) AS d (domain, code)
         JOIN marketplaces m ON m.code = d.code;