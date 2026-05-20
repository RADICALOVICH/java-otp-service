# OTP Service

Backend-сервис защиты операций одноразовыми кодами.

Клиент дёргает API: «начни операцию `X`, выдай код юзеру», получает код по выбранному каналу, отдаёт обратно для подтверждения.

## Возможности

- Регистрация и логин (BCrypt + JWT с TTL)
- Роли `ADMIN` (один на всю систему, гарантия БД) и `USER`
- Генерация OTP, привязанного к `operationId`
- 4 канала доставки: **EMAIL** (Angus Mail), **SMS** (SMPP/SMPPsim), **TELEGRAM** (Bot API), **FILE** (запись в файл)
- Валидация кода с проверкой статуса, срока, владельца
- Фоновый scheduler гасит просроченные коды
- Admin API: конфиг длины/TTL, список юзеров, удаление (с каскадом по OTP)
- Единый формат ошибок, request-логирование, бизнес-логирование в сервисах

## Стек

| | |
|---|---|
| Java | 21 |
| Spring Boot | 3.4 (Spring MVC) |
| PostgreSQL | 17 + JDBC (без JPA) |
| Flyway | миграции |
| JWT | jjwt 0.12 |
| BCrypt | at.favre.lib |
| Email / SMS / Telegram | Angus Mail / jsmpp / `java.net.http.HttpClient` |
| Сборка | Maven |
| Запуск | Docker Compose |

## Структура пакетов (package by feature)

```
src/main/java/com/vpoluboyarov/otp/
├── OtpServiceApplication.java
├── auth/      регистрация, логин, JWT, фильтр аутентификации, @AdminOnly
├── user/      User, Role, UserDao, UserResponse
├── otp/       генерация/валидация OTP, scheduler, notification/* (4 канала)
├── admin/     admin endpoints (@AdminOnly)
└── shared/    ApiError, GlobalExceptionHandler, SecurityContext, RequestLoggingFilter, exceptions
```

## Запуск

Нужен **Docker Desktop**. Дальше:

```bash
git clone <repo-url>
cd Java-OTP
cp .env.example .env

# Конфиги каналов. Без них app не стартует (Spring @PropertySource требует файлы).
# В .example уже заданы дефолты, которых хватает чтобы app поднялся;
# для реальной рассылки по EMAIL/TELEGRAM подставить креды — см. «Настройка каналов».
cp src/main/resources/email.properties.example    src/main/resources/email.properties
cp src/main/resources/sms.properties.example      src/main/resources/sms.properties
cp src/main/resources/telegram.properties.example src/main/resources/telegram.properties

docker compose up -d
curl http://localhost:8080/api/ping       # → pong
```

Поднимется три контейнера: `db` (Postgres), `smppsim` (эмулятор SMS), `app` (наше приложение). Приложение на `http://localhost:8080/api`.

Чтобы пересоздать БД с нуля: `docker compose down -v && docker compose up -d`.

## API

Префикс — `/api`. Защищённые ручки требуют `Authorization: Bearer <jwt>`. Публичные: `/api/ping`, `/api/auth/register`, `/api/auth/login`.

**Формат ошибок (одинаковый для всех):**

```json
{ "timestamp":"...", "status":400, "error":"BAD_REQUEST", "message":"...", "path":"/api/..." }
```

### `GET /api/ping` — health
```bash
curl http://localhost:8080/api/ping
```
- `200` → `pong`

### `POST /api/auth/register`
Body: `login`, `password` (≥ 6 символов), `role` (`USER`/`ADMIN`), опц. `email`, `phone`, `telegramChatId`.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"vasya","password":"qwerty","role":"USER","email":"v@y.ru"}'
```
- `201` — `{ id, login, role }`
- `409` — логин занят / второй админ
- `400` — валидация

### `POST /api/auth/login`
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"vasya","password":"qwerty"}'
```
- `200` — `{ token, expiresAt, user: {...} }`
- `401` — `invalid credentials` (единое сообщение)

### `POST /api/otp/generate` — любая роль
Body: `operationId` (≤ 100), `channel` (`EMAIL`/`SMS`/`TELEGRAM`/`FILE`).

```bash
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"operationId":"transfer-42","channel":"FILE"}'
```
- `201` — `{ codeId, operationId, channel, expiresAt }`. **Самого кода нет** — он уходит по каналу.
- `400` — у юзера не задан контакт для выбранного канала
- `409` — для `(user, operationId)` уже есть `ACTIVE`-код

### `POST /api/otp/validate` — любая роль
```bash
curl -X POST http://localhost:8080/api/otp/validate \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"operationId":"transfer-42","code":"123456"}'
```
- `200` — `{ "status": "OK" }`, код переведён в `USED`
- `400` — `invalid or expired code` (единое сообщение для всех негативных случаев)

### Admin (роль `ADMIN`)

| Метод | Путь | Что |
|---|---|---|
| `GET` | `/api/admin/config` | текущий конфиг `{ codeLength, ttlSeconds, updatedAt }` |
| `PATCH` | `/api/admin/config` | изменить `codeLength` (4..10) и/или `ttlSeconds` (>0) |
| `GET` | `/api/admin/users` | список не-админов |
| `DELETE` | `/api/admin/users/{id}` | удалить юзера (`204`); `403` — себя нельзя; `404` — не найден |

USER на любую из них → `403 admin only`.

## Схема БД

Создаётся Flyway-миграцией `V1__init.sql` при старте.

```
users  ──< 1 — N >──  otp_codes      (FK с ON DELETE CASCADE)
otp_config (всегда 1 строка)
```

Ключевые гарантии на уровне БД:
- **Один админ** — партиальный уникальный индекс `WHERE role = 'ADMIN'`.
- **Не больше одного `ACTIVE`-кода на `(user, operationId)`** — партиальный уникальный индекс `WHERE status = 'ACTIVE'` (защита от race).
- **`otp_config` — ровно 1 строка** — `PRIMARY KEY CHECK (id = 1)`.
- **Каскадное удаление кодов** при удалении юзера.
- **`TIMESTAMPTZ` везде** — хранение в UTC.

## Настройка каналов

**FILE** — работает сразу, код пишется в `/app/otp-codes.txt` внутри контейнера:
```bash
docker compose exec app cat /app/otp-codes.txt
```

**EMAIL** — Gmail с app-password:
1. Включить 2FA в Google аккаунте, создать App Password.
2. `cp src/main/resources/email.properties.example src/main/resources/email.properties` и подставить креды.
3. `docker compose up -d --build app`.

**TELEGRAM** — бот через `@BotFather`:
1. `/newbot` у `@BotFather`, получить токен.
2. Открыть бота, нажать **Start** (иначе диалог не создастся).
3. Открыть `https://api.telegram.org/bot<ТОКЕН>/getUpdates` — найти `chat.id`.
4. `cp src/main/resources/telegram.properties.example src/main/resources/telegram.properties`, подставить токен.
5. При регистрации юзера передавать `telegramChatId`.
6. `docker compose up -d --build app`.

**SMS** — SMPPsim. Дефолты из `sms.properties.example` подходят к контейнеру `smppsim` из `docker-compose.yml` — после копирования файла (шаг в «Запуск») работает без правок.

## Полный сценарий теста

```bash
# Зарегать админа и юзера
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"login":"boss","password":"adminpass","role":"ADMIN"}'
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"login":"vasya","password":"qwerty","role":"USER"}'

# Логин юзера → токен
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"vasya","password":"qwerty"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"$//')

# Сгенерить код по FILE
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"operationId":"demo-1","channel":"FILE"}'

# Достать код из файла и валидировать
CODE=$(docker compose exec app tail -1 /app/otp-codes.txt | grep -o 'code=[0-9]*' | cut -d= -f2)
curl -X POST http://localhost:8080/api/otp/validate \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d "{\"operationId\":\"demo-1\",\"code\":\"$CODE\"}"
```
