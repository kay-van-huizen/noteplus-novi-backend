---
description: Maakt Flyway migratiescripts aan voor seed data in NotePlus. Leest de entiteiten, genereert V1__seed_roles.sql en V2__seed_users.sql met correcte UUIDs en junction table inserts.
allowed-tools: Read, Write, Glob, Grep
---

# Genereer Flyway seed data

## Stap 1 — Lees de entiteiten en application.properties

Lees alle files in `src/main/java/org/noteplus/noteplus/entity/` en
`src/main/resources/application.properties`.

Controleer:
- Wat is het id type van BaseEntity? (verwacht: UUID)
- Hoe heet de junction tabel voor User ↔ Role?
- Welke kolommen zijn NOT NULL op elke tabel?

## Stap 2 — Zorg dat Flyway correct geconfigureerd is

Controleer application.properties op:
- spring.flyway.enabled=true
- spring.flyway.locations=classpath:db/migration
- spring.jpa.hibernate.ddl-auto=update
- spring.jpa.defer-datasource-initialization=true
- spring.sql.init.mode=never

Pas aan waar nodig.

## Stap 3 — Maak de migratiescripts

Maak de directory `src/main/resources/db/migration/` als die niet bestaat.

### V1__seed_roles.sql
```sql
INSERT INTO roles (id, name, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'ROLE_ADMIN',   NOW(), NOW()),
  (gen_random_uuid(), 'ROLE_STUDENT', NOW(), NOW()),
  (gen_random_uuid(), 'ROLE_COACH',   NOW(), NOW());
```

### V2__seed_users.sql
Gebruik placeholder hashes — de developer vervangt deze met echte BCrypt output:
```sql
INSERT INTO users (id, username, email, password, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'admin',    'admin@noteplus.nl',   '$2a$10$', NOW(), NOW()),
  (gen_random_uuid(), 'student1', 'student@noteplus.nl', '$2a$10$', NOW(), NOW()),
  (gen_random_uuid(), 'coach1',   'coach@noteplus.nl',   '$2a$10$', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'student1' AND r.name = 'ROLE_STUDENT';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'coach1' AND r.name = 'ROLE_COACH';
```
