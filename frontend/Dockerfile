FROM nginx:1.27-alpine

# Шаблон настроек: при старте контейнера nginx подставит в него переменные окружения
# (BACKEND_URL) и положит готовый файл в /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

COPY index.html /usr/share/nginx/html/
COPY css /usr/share/nginx/html/css
COPY js /usr/share/nginx/html/js

# Файлы копируются с правами с компьютера разработчика. Если они «только для владельца»,
# nginx (он работает не от root) не сможет их прочитать и ответит 403. Открываем всем на чтение.
RUN chmod -R a+rX /usr/share/nginx/html

# Куда пересылать /api: по умолчанию — контейнер бэкенда в docker compose
ENV BACKEND_URL=http://backend:8080