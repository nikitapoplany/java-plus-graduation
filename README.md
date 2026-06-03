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
- `main-service` - сохранен как совместимый исходный слой предыдущей реализации, но не используется как runtime-сервис в `docker-compose`.

На текущем этапе доменные сервисы собираются как отдельные Spring Boot приложения и регистрируются в Eureka. Чтобы сохранить совместимость старого API и тестов во время постепенного разделения, сервисы используют общий исходный слой из `main-service` и общую основную БД. При этом новые приложения ограничивают component scan по своим зонам ответственности: `user-service` поднимает пользовательский API, `request-service` - API заявок, `comment-service` - API комментариев, а `event-service` отвечает за события, категории и подборки. Часть прямых JPA-зависимостей уже заменена на Feign-вызовы: заявки проверяют пользователя и событие через внутренний API, комментарии проверяют пользователя и событие через внутренний API, а события получают счетчики подтвержденных заявок из `request-service` с fallback на `0`.

JPA-модель пока сохраняет связи между `events`, `users`, `requests` и `comments`; это осознанный промежуточный шаг. Следующая итерация разделения данных должна заменить связи на id/snapshot-поля и закрепить владение таблицами за отдельными сервисами.

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
- `POST /internal/events/{eventId}/confirmed-requests?delta=N` - изменить счетчик подтвержденных заявок.

`REQUEST-SERVICE`:

- `GET /internal/requests/events/{eventId}/confirmed-count` - получить количество подтвержденных заявок по данным сервиса заявок.
- `GET /internal/requests/events/confirmed-counts?eventIds=1&eventIds=2` - получить счетчики подтвержденных заявок для списка событий одним запросом.

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
