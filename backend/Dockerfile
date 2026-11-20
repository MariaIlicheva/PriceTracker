# syntax=docker/dockerfile:1

# ---------- Этап 1: сборка ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# --mount=type=cache: папка с библиотеками Maven (~/.m2) хранится между сборками.
# Скачанное однажды не качается снова — даже если прошлая сборка упала на середине.
# retryHandler.count: при обрыве соединения Maven повторит загрузку файла до 10 раз.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests -Daether.connector.http.retryHandler.count=10

# ---------- Этап 2: запуск ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Приложение работает не от root, а от обычного пользователя
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]