---
description: Maakt een git commit met een conventionele commit message. Gebruik: /commit <message> of /commit (dan kiest Claude zelf een message op basis van de wijzigingen)
allowed-tools: Bash(git add:*), Bash(git commit:*), Bash(git diff:*), Bash(git status:*)
argument-hint: [commit message]
---

# Git Commit — NotePlus

Maak een nette git commit. De school vereist minimaal 20 kleine, beschrijvende commits.

## Stap 1 — Bekijk de wijzigingen

```bash
git status
git diff --cached
git diff
```

## Stap 2 — Stage de juiste bestanden

Stage alleen gerelateerde bestanden samen (één feature per commit):
```bash
git add <specifieke bestanden>
```

Nooit `git add .` gebruiken als er niet-gerelateerde wijzigingen zijn.

## Stap 3 — Commit message

Als `$ARGUMENTS` is opgegeven, gebruik dat als basis.
Anders, genereer zelf een message op basis van de diff.

Gebruik **Conventional Commits** formaat:
```
<type>: <korte beschrijving in het Nederlands of Engels>
```

Types:
- `feat:` — nieuwe functionaliteit
- `fix:` — bugfix
- `test:` — tests toevoegen/aanpassen
- `docs:` — documentatie
- `refactor:` — code verbetering zonder feature change
- `chore:` — build, config, dependencies

Voorbeelden:
```
feat: add NoteController with CRUD endpoints
feat: implement NoteServiceImpl with owner validation
test: add unit tests for NoteServiceImpl with 100% coverage
feat: add file upload endpoint for Reference attachments
fix: fix 403 error when accessing other user's notes
docs: update CLAUDE.md with new service patterns
```

## Stap 4 — Commit

```bash
git commit -m "<generated message>"
```

Houd de message kort (max 72 tekens), beschrijvend en in tegenwoordige tijd.
