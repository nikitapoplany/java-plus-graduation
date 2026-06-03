# Explore With Me Plus

Проект переведен на микросервисную структуру поверх инфраструктуры Spring Cloud.
Внешний API остается доступен через API Gateway на порту `8080`.

## Архитектура

Инфраструктурные сервисы находятся в модуле `infra`:

- `discovery-server` - Eureka Service Discovery.
- `config-server` - Spring Cloud Config Server, native-конфиги лежат в `infra/config-server/src/main/resources/configs`.
- `gateway-server` - единая точка входа для внешнего API.

Доменные сервисы находятся в модуле `core`:

- `user-service` - административное управление пользователями.
- `event-service` - события, категории и подборки.
- `request-service` - заявки на участие в событиях.
- `comment-service` - комментарии как дополнительная функциональность.
- `main-service` - сохранен как совместимый слой предыдущей реализации.

На текущем этапе доменные сервисы собираются как отдельные Spring Boot приложения и регистрируются в Eureka. Чтобы сохранить совместимость старого API и тестов во время постепенного разделения, сервисы используют общий исходный слой из `main-service` и общую основную БД. Внутренние HTTP-контракты уже добавлены, чтобы дальше можно было по одному заменять прямые JPA-зависимости на Feign-вызовы.

## Gateway Routes

Gateway получает маршруты из `gateway-server.yml`:

- `/admin/users`, `/admin/users/**` -> `USER-SERVICE`
- `/users/*/requests`, `/users/*/requests/**` -> `REQUEST-SERVICE`
- `/users/*/events/*/requests`, `/users/*/events/*/requests/**` -> `REQUEST-SERVICE`
- `/admin/events/comments`, `/admin/events/comments/**` -> `COMMENT-SERVICE`
- `/users/*/events/*/comments`, `/users/*/events/*/comments/**` -> `COMMENT-SERVICE`
- `/events/*/comments`, `/events/*/comments/**` -> `COMMENT-SERVICE`
- `/admin/categories/**`, `/admin/compilations/**`, `/admin/events/**` -> `EVENT-SERVICE`
- `/users/*/events/**` -> `EVENT-SERVICE`
- `/events/**`, `/categories/**`, `/compilations/**` -> `EVENT-SERVICE`

Порядок маршрутов важен: более специфичные маршруты заявок и комментариев должны идти выше общих маршрутов событий.

## Internal API

Внутренние endpoint'ы предназначены для межсервисного взаимодействия через OpenFeign.

`USER-SERVICE`:

- `GET /internal/users/{userId}` - получить пользователя.
- `GET /internal/users/{userId}/exists` - проверить существование пользователя.

`EVENT-SERVICE`:

- `GET /internal/events/{eventId}` - получить событие.
- `GET /internal/events/{eventId}/exists` - проверить существование события.
- `GET /internal/events/{eventId}/initiator` - получить id инициатора события.
- `GET /internal/events/{eventId}/confirmed-requests` - получить количество подтвержденных заявок из события.

`REQUEST-SERVICE`:

- `GET /internal/requests/events/{eventId}/confirmed-count` - получить количество подтвержденных заявок по данным сервиса заявок.

Feign-клиенты находятся в пакетах `ru.practicum.internal.client` соответствующих сервисов.

## Конфигурация

Все сервисы получают настройки через Config Server:

- `user-service.yml`
- `event-service.yml`
- `request-service.yml`
- `comment-service.yml`
- `stats-server.yml`
- `gateway-server.yml`
- `main-service.yml`

Для CI значения datasource берутся из `SPRING_DATASOURCE_*` или `POSTGRES_*`. По умолчанию используется `jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/ewm_main_db` для доменных сервисов и `ewm_stats_db` для сервиса статистики.

## Локальный запуск

Через Docker Compose:

```bash
docker compose up --build
```

После запуска внешний API доступен на:

```text
http://localhost:8080
```

## Проверка

Базовая проверка сборки:

```bash
mvn clean install -DskipTests --no-transfer-progress
```

Внешняя спецификация API находится в файлах:

- `ewm-main-service-spec.json`
- `ewm-stats-service-spec.json`
