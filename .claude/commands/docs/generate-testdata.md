---
description: Genereert een data.sql bestand met testdata voor de NotePlus database. Maakt gebruikers aan voor alle rollen (ADMIN, STUDENT, COACH) en vult de tabellen met voorbeelddata.
allowed-tools: Read, Write, Glob, Grep
---

# Genereer data.sql testdata

De school vereist dat het systeem geleverd wordt met een valide set testdata
die automatisch in de database wordt gezet (criterium 3.5).

## Stap 1 — Lees de entiteiten

Lees de volgende bestanden om de exacte tabelnamen en kolommen te bepalen:
- Alle files in `src/main/java/org/noteplus/noteplus/entity/`
- `src/main/resources/application.properties` (voor ddl-auto instelling)

## Stap 2 — Genereer de data.sql

Maak `src/main/resources/data.sql` aan met:

### Gebruikers (alle 3 rollen!)
```sql
-- Wachtwoord: 'password123' als BCrypt hash
INSERT INTO users (id, username, email, password, created_at, updated_at)
VALUES
  (1, 'admin', 'admin@noteplus.nl', '$2a$10$...bcrypt hash...', NOW(), NOW()),
  (2, 'student1', 'student@noteplus.nl', '$2a$10$...bcrypt hash...', NOW(), NOW()),
  (3, 'coach1', 'coach@noteplus.nl', '$2a$10$...bcrypt hash...', NOW(), NOW());
```

Genereer de BCrypt hash voor 'password123' via:
```java
System.out.println(new BCryptPasswordEncoder().encode("password123"));
```

### Rollen
```sql
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN'), (2, 'ROLE_STUDENT'), (3, 'ROLE_COACH');
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1), (2, 2), (3, 3);
```

### Categorieën (met parent-child relatie!)
```sql
INSERT INTO categories (id, name, parent_id, created_at, updated_at)
VALUES
  (1, 'Programmeren', NULL, NOW(), NOW()),
  (2, 'Java', 1, NOW(), NOW()),
  (3, 'Spring Boot', 2, NOW(), NOW());
```

### Notes
```sql
INSERT INTO notes (id, title, content, user_id, category_id, deleted_at, created_at, updated_at)
VALUES
  (1, 'Introductie Java', 'Java is een OOP taal...', 2, 2, NULL, NOW(), NOW()),
  (2, 'Spring Boot basics', 'Spring Boot maakt...', 2, 3, NULL, NOW(), NOW());
```

### LearningPaths
```sql
INSERT INTO learning_paths (id, name, description, user_id, created_at, updated_at)
VALUES
  (1, 'Java Backend Developer', 'Leerpad voor backend...', 3, NOW(), NOW());
```

## Stap 3 — Installeer de data

Zorg dat Spring Boot de data.sql laadt bij opstarten:
```properties
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

## Stap 4 — Documenteer de testgebruikers

Voeg ook toe aan de `Installatiehandleiding.docx` onder sectie "Testgebruikers":

| Gebruiker | Email | Wachtwoord | Rol |
|-----------|-------|-----------|-----|
| admin | admin@noteplus.nl | password123 | ADMIN |
| student1 | student@noteplus.nl | password123 | STUDENT |
| coach1 | coach@noteplus.nl | password123 | COACH |
