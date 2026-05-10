---
description: Controleert de codebase en documenten tegen de schoolopdracht vereisten. Geeft een gedetailleerd rapport van wat klaar is en wat nog mist. Geen argumenten nodig.
allowed-tools: Read, Glob, Grep, Bash(./mvnw test:*)
---

# Opdracht Quickscan — NotePlus

Controleer de volledige codebase tegen de vereisten van de Novi Hogeschool Backend eindopdracht (v3.5).

## Stap 1 — Scan de codebase

Gebruik Glob en Grep om te controleren:

```
src/main/java/org/noteplus/noteplus/controller/    ← controllers aanwezig?
src/main/java/org/noteplus/noteplus/service/       ← services aanwezig?
src/main/java/org/noteplus/noteplus/service/impl/  ← implementaties aanwezig?
src/main/java/org/noteplus/noteplus/dto/           ← DTOs aanwezig?
src/main/java/org/noteplus/noteplus/exception/     ← exceptions aanwezig?
src/main/resources/data.sql                         ← testdata aanwezig?
src/test/java/                                      ← tests aanwezig?
```

## Stap 2 — Check elke eis

Geef voor elk punt ✅ (klaar), ⚠️ (gedeeltelijk) of ❌ (ontbreekt):

### Broncode (65%)
- [ ] 3 kernfunctionaliteiten geïmplementeerd (naast auth)
- [ ] CLEAN code en SOLID toegepast (services als interfaces)
- [ ] Minimaal 2 userrollen met verschillende rechten (`@PreAuthorize`)
- [ ] Exception handling centraal via `@ControllerAdvice`
- [ ] API en database onafhankelijk (repository pattern)
- [ ] CRUD operaties via repositories
- [ ] Minimaal 1 one-to-one relatie in database
- [ ] File upload EN download endpoint
- [ ] JWT authenticatie + rolgebaseerde autorisatie
- [ ] 2 service klassen getest met 100% line coverage
- [ ] Minimaal 10 unit tests (Arrange-Act-Assert)
- [ ] Minimaal 2 integratietests (MockMvc)
- [ ] Flyway migrations V1 + V2 aanwezig met seed data (roles + users)
- [ ] Minimaal 20 commits
- [ ] Minimaal 5 pull requests naar main

### Documenten
- [ ] Technisch Ontwerp: titelblad, inhoudsopgave, probleembeschrijving
- [ ] Technisch Ontwerp: 4+ user stories
- [ ] Technisch Ontwerp: 25+ functionele + niet-functionele eisen
- [ ] Technisch Ontwerp: klassendiagram met kardinaliteiten
- [ ] Technisch Ontwerp: 2 sequentiediagrammen (controller→service→repository)
- [ ] Verantwoordingsdocument: 5 technische keuzes met onderbouwing
- [ ] Verantwoordingsdocument: 5 limitaties + doorontwikkelingen
- [ ] Verantwoordingsdocument: GitHub link
- [ ] Installatiehandleiding: volledig (benodigdheden, stappenplan, users, endpoints)
- [ ] Postman collectie (.json)

### Quickscan (inleververeisten)
- [ ] Alle documentatie als .pdf
- [ ] ZIP bestand (niet .rar), max 50MB
- [ ] Broncode zonder target/, .idea/, .iml
- [ ] API start op zonder te crashen

## Stap 3 — Rapport

Geef een duidelijk rapport met:
1. Overzicht van wat ✅ klaar is
2. Lijst van ❌ ontbrekende onderdelen (gesorteerd op impact)
3. Aanbevolen volgorde om de ontbrekende onderdelen te implementeren
4. Schatting van de resterende werktijd

## Stap 4 — Optioneel: Run tests

```bash
./mvnw test 2>&1 | tail -20
```

Laat zien hoeveel tests slagen en of er errors zijn.
