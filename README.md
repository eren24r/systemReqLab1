# UniCoFound — платформа студенческих стартапов (лабораторная №1)

Монолит на Spring Boot 3.5 / Java 17 / Maven. База данных — PostgreSQL, схема создаётся Liquibase-миграциями.

## Запуск

```bash
docker compose up --build
```

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Переменные окружения

| Переменная    | Значение по умолчанию                         | Описание                 |
|---------------|-----------------------------------------------|--------------------------|
| `SERVER_PORT` | `8080`                                        | порт приложения          |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/unicofound` | JDBC URL                 |
| `DB_USERNAME` | `unicofound`                                  | пользователь БД          |
| `DB_PASSWORD` | `unicofound`                                  | пароль БД                |
| `DB_SCHEMA`   | `public`                                      | схема для таблиц и Liquibase |

Запуск против учебной БД (helios) через ssh-туннель `ssh s373751@se.ifmo.ru -L 5432:bd:5432`:

```bash
DB_URL=jdbc:postgresql://localhost:5432/studs DB_USERNAME=s373751 DB_PASSWORD=... DB_SCHEMA=s373751 ./mvnw spring-boot:run
```

### Тесты и покрытие

```bash
./mvnw verify
```

Интеграционные тесты поднимают PostgreSQL через Testcontainers (нужен запущенный Docker). JaCoCo-отчёт: `target/site/jacoco/index.html`, порог покрытия 70% проверяется на фазе `verify`.

Только модульные тесты (без Docker):

```bash
./mvnw test -Dtest="*ServiceTest,PaginationTest"
```

## Предметная область и схема БД

| Таблица                   | Назначение                                                  | Связь                                |
|---------------------------|-------------------------------------------------------------|--------------------------------------|
| `users`                   | студенты, инвесторы, администраторы (`role`)                |                                      |
| `skills`                  | справочник навыков (`category`)                             |                                      |
| `user_skills`             | навыки пользователя                                         | Many-to-Many (users ↔ skills)        |
| `projects`                | стартап-проекты, `owner_id` → users                         | Many-to-One (project → owner)        |
| `project_required_skills` | требуемые проекту навыки                                    | Many-to-Many (projects ↔ skills)     |
| `team_memberships`        | участники команды: `role`, `equity_share`, `joined_at`      | Many-to-Many с доп. полями (users ↔ projects) |
| `join_requests`           | заявки на вступление в команду (`status`)                   | One-to-Many (project → requests)     |
| `investment_offers`       | инвестиционные предложения (`amount`, `equity_percent`, `status`) | One-to-Many (project → offers) |

Все enum хранятся строками (`VARCHAR` + `CHECK`-ограничения в миграциях, `@Enumerated(EnumType.STRING)` в сущностях).

## REST API

| Метод  | Путь                                       | Описание                                            |
|--------|--------------------------------------------|-----------------------------------------------------|
| POST   | `/api/users`                               | создать пользователя (201)                          |
| GET    | `/api/users?page=&size=`                   | список, общее число в заголовке `X-Total-Count`     |
| GET/PUT/DELETE | `/api/users/{id}`                  | получить / обновить / удалить (204)                 |
| POST/DELETE | `/api/users/{id}/skills/{skillId}`    | добавить / убрать навык                             |
| POST   | `/api/skills`                              | создать навык                                       |
| GET    | `/api/skills?page=&size=`                  | список с `X-Total-Count`                            |
| GET/PUT/DELETE | `/api/skills/{id}`                 |                                                     |
| POST   | `/api/projects`                            | создать проект, владелец становится `FOUNDER`       |
| GET    | `/api/projects?status=&page=&size=`        | лента проектов — бесконечная прокрутка (`hasNext`, без общего числа) |
| GET/PUT/DELETE | `/api/projects/{id}`               | смена статуса `DRAFT → PUBLISHED → ARCHIVED` через PUT |
| GET    | `/api/projects/{id}/members`               | команда — бесконечная прокрутка                     |
| PATCH/DELETE | `/api/projects/{id}/members/{userId}` | роль и доля участника / исключение из команды     |
| POST   | `/api/join-requests`                       | заявка на вступление                                |
| GET    | `/api/join-requests?projectId=&applicantId=` | список с `X-Total-Count`                          |
| POST   | `/api/join-requests/{id}/accept|reject`    | принять / отклонить заявку                          |
| POST   | `/api/investment-offers`                   | инвестиционное предложение                          |
| GET    | `/api/investment-offers?projectId=&investorId=` | список с `X-Total-Count`                       |
| POST   | `/api/investment-offers/{id}/accept|reject|withdraw` | принять / отклонить / отозвать            |

Размер страницы ограничен 50 записями: `size > 50` → `400 Bad Request`.

### HTTP-статусы

- `201` создание, `204` удаление, `200` остальные успешные.
- `400` ошибки валидации тела/параметров (поля в `details`), некорректный JSON или enum.
- `404` сущность не найдена.
- `409` конфликт: дубликат e-mail/навыка, повторная обработка заявки/предложения, нарушение целостности БД.
- `422` нарушение бизнес-правила (инвестор не может владеть проектом, заявки только к опубликованным проектам и т.д.).

Тело ошибки: `{timestamp, status, error, message, path, details}`.

## Транзакции в сложных операциях

1. **`ProjectService.create`** — вставка в `projects`, `team_memberships` (владелец как `FOUNDER`) и `project_required_skills`. Без транзакции при сбое на втором шаге появился бы проект без основателя.
2. **`TeamService.acceptJoinRequest`** — вставка в `team_memberships` и обновление статуса в `join_requests`. Если вставка участника нарушит уникальность (`project_id, user_id`), заявка не должна остаться в статусе `ACCEPTED`; обе записи меняются атомарно.
3. **`InvestmentService.accept`** — обновление статуса предложения, увеличение `raised_amount` проекта, при достижении цели смена статуса проекта на `FUNDED` и отклонение всех остальных `PENDING`-предложений. Это read-modify-write нескольких строк в двух таблицах: частичное применение привело бы к расхождению суммы и статусов. Дополнительно `@Version` на `projects` защищает от потерянных обновлений при параллельном принятии предложений.

## Валидация

- Уровень контроллера: `@Valid` на DTO-записях (`jakarta.validation`), `@Min/@Max` на параметрах пагинации.
- Уровень сущности: аннотации `@NotBlank`, `@Email`, `@Size`, `@DecimalMin` и др. на полях Entity, проверяются Hibernate перед записью.
- Уровень БД: `NOT NULL`, `UNIQUE`, `CHECK`, внешние ключи в миграциях.

## Git

Feature-branching: `feature/domain-model`, `feature/rest-api`, `feature/tests`, `feature/docker-and-docs` сливаются в `main` через merge-коммиты; сообщения в формате Conventional Commits.

## Postman

Коллекция `postman/UniCoFound.postman_collection.json` содержит сквозной сценарий проверки (переменная `baseUrl` по умолчанию `http://localhost:8080`).
