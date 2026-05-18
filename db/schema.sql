-- ============================================================================
-- otp-service — initial schema (PostgreSQL 17)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- USERS
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login             VARCHAR(50)  NOT NULL UNIQUE,
    password_hash     VARCHAR(72)  NOT NULL,
    role              VARCHAR(10)  NOT NULL
                          CHECK (role IN ('ADMIN', 'USER')),
    email             VARCHAR(255),
    phone             VARCHAR(20),
    telegram_chat_id  VARCHAR(50),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Гарантия «один админ»: частичный уникальный индекс по role='ADMIN'.
CREATE UNIQUE INDEX one_admin
    ON users (role)
    WHERE role = 'ADMIN';


-- ----------------------------------------------------------------------------
-- OTP CONFIG (всегда ровно одна строка, id = 1)
-- ----------------------------------------------------------------------------
CREATE TABLE otp_config (
    id            SMALLINT     PRIMARY KEY CHECK (id = 1),
    code_length   INT          NOT NULL CHECK (code_length BETWEEN 4 AND 10),
    ttl_seconds   INT          NOT NULL CHECK (ttl_seconds > 0),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Дефолтная конфигурация (6 цифр, время жизни 5 минут).
INSERT INTO otp_config (id, code_length, ttl_seconds)
VALUES (1, 6, 300);


-- ----------------------------------------------------------------------------
-- OTP CODES
-- ----------------------------------------------------------------------------
CREATE TABLE otp_codes (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT       NOT NULL
                      REFERENCES users(id) ON DELETE CASCADE,
    operation_id  VARCHAR(100) NOT NULL,
    code          VARCHAR(10)  NOT NULL,
    status        VARCHAR(10)  NOT NULL
                      CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    channel       VARCHAR(10)  NOT NULL
                      CHECK (channel IN ('EMAIL', 'SMS', 'TELEGRAM', 'FILE')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL
);

-- Не более одного ACTIVE-кода на пару (user_id, operation_id).
-- Реализует «нельзя дубли» атомарно на уровне БД.
CREATE UNIQUE INDEX one_active_code_per_operation
    ON otp_codes (user_id, operation_id)
    WHERE status = 'ACTIVE';

-- Под scheduler: быстрый поиск просроченных активных кодов.
CREATE INDEX idx_otp_codes_active_expires
    ON otp_codes (expires_at)
    WHERE status = 'ACTIVE';
