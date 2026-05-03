---
description: Maakt een feature branch aan en opent daarna een pull request naar main. Gebruik: /branch <feature-naam> (bijv. /branch note-crud)
allowed-tools: Bash(git checkout:*), Bash(git branch:*), Bash(git push:*), Bash(gh pr create:*)
argument-hint: <feature-naam>
---

# Feature Branch — $ARGUMENTS

De school vereist minimaal 5 pull requests (criterium 3.4).
Maak voor iedere nieuwe feature een aparte branch en PR.

## Stap 1 — Zorg dat main up-to-date is

```bash
git checkout main
git pull origin main
```

## Stap 2 — Maak de feature branch

```bash
git checkout -b feat/$ARGUMENTS
```

Branch naamgeving: `feat/` prefix + kebab-case beschrijving
Voorbeelden: `feat/note-crud`, `feat/auth-endpoints`, `feat/file-upload`, `feat/learning-path`

## Stap 3 — Meld aan gebruiker

Laat weten dat de branch klaarstaat en geef aan:
- Wat de volgende stap is (bijv. `/new-feature Note` om de code te scaffolden)
- Hoe de PR later aangemaakt wordt (`/pr <beschrijving>`)

## Na het coderen — PR aanmaken

Gebruik `/git:pr` om de pull request naar main te pushen.
