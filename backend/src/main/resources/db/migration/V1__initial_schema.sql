-- Initial schema, from the data model in docs/PLAN.md.
-- Never edit this file once it has run anywhere: add V2__..., V3__... instead.

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(30)  NOT NULL UNIQUE,
    role            VARCHAR(10)  NOT NULL,             -- CADDIE / ADMIN
    password_hash   VARCHAR(100) NULL,                 -- NULL until first login sets it
    push_token      VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE caddies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE,
    grade           VARCHAR(10) NOT NULL,              -- SINGLE / DOUBLE / JUNIOR
    is_priority     BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_caddies_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE attendance (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    caddie_id       BIGINT   NOT NULL,
    date            DATE     NOT NULL,
    checked_in_at   DATETIME NULL,
    checked_out_at  DATETIME NULL,
    CONSTRAINT fk_attendance_caddie FOREIGN KEY (caddie_id) REFERENCES caddies (id),
    CONSTRAINT uq_attendance_day UNIQUE (caddie_id, date)
);

-- The one queue. Singles/doubles boards are filtered views of this table.
CREATE TABLE queue_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    date            DATE        NOT NULL,
    caddie_id       BIGINT      NOT NULL,
    position        INT         NOT NULL,
    status          VARCHAR(10) NOT NULL,              -- OFF / AVAILABLE / CALLED / OUT / BREAK
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_caddie FOREIGN KEY (caddie_id) REFERENCES caddies (id),
    CONSTRAINT uq_queue_day_caddie UNIQUE (date, caddie_id),
    INDEX ix_queue_day_position (date, position)
);

CREATE TABLE loops (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    caddie_id       BIGINT      NOT NULL,
    date            DATE        NOT NULL,
    bag_type        VARCHAR(10) NOT NULL,              -- SINGLE / DOUBLE
    tee_time        TIME        NULL,
    called_at       DATETIME    NULL,
    accepted_at     DATETIME    NULL,
    assigned_by     BIGINT      NOT NULL,
    CONSTRAINT fk_loops_caddie FOREIGN KEY (caddie_id) REFERENCES caddies (id),
    CONSTRAINT fk_loops_admin  FOREIGN KEY (assigned_by) REFERENCES users (id)
);

CREATE TABLE absences (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    caddie_id       BIGINT       NOT NULL,
    date            DATE         NOT NULL,
    reason          VARCHAR(500) NULL,
    submitted_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved        BOOLEAN      NULL,                 -- NULL = waiting for admin
    CONSTRAINT fk_absences_caddie FOREIGN KEY (caddie_id) REFERENCES caddies (id)
);

CREATE TABLE issue_reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    caddie_id       BIGINT        NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    hole            INT           NULL,
    message         VARCHAR(1000) NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issues_caddie FOREIGN KEY (caddie_id) REFERENCES caddies (id)
);

CREATE TABLE audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id        BIGINT      NOT NULL,
    action          VARCHAR(30) NOT NULL,
    before_state    JSON        NULL,
    after_state     JSON        NULL,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES users (id)
);
