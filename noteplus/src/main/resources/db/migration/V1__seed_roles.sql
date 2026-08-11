-- V1__seed_roles.sql
-- Creates the initial roles table (integer PK — superseded by V5's UUID redesign)
-- and seeds the three application roles.

CREATE TABLE roles (
    id         SERIAL      NOT NULL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);

INSERT INTO roles (name, created_at, updated_at)
VALUES
    ('ROLE_ADMIN',   NOW(), NOW()),
    ('ROLE_STUDENT', NOW(), NOW()),
    ('ROLE_COACH',   NOW(), NOW());