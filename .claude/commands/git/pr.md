---
description: Push de huidige branch en maak een pull request aan naar main. Gebruik: /pr <beschrijving> (bijv. /pr "Note CRUD endpoints geïmplementeerd")
allowed-tools: Bash(git push:*), Bash(git status:*), Bash(gh pr create:*), Bash(git log:*)
argument-hint: <PR beschrijving>
---

# Pull Request Aanmaken — $ARGUMENTS

## Stap 1 — Controleer de huidige status

```bash
git status
git log --oneline main..HEAD
```

Zorg dat alle wijzigingen gecommit zijn (gebruik `/git:commit` als dat nog niet is gedaan).

## Stap 2 — Push de branch

```bash
git push -u origin HEAD
```

## Stap 3 — Maak de PR aan

```bash
gh pr create \
  --title "$ARGUMENTS" \
  --body "## Wat is er gedaan?

$ARGUMENTS

## Checklist
- [ ] Code compileert zonder errors
- [ ] Unit tests geschreven en geslaagd
- [ ] Swagger annotations aanwezig
- [ ] DTOs valideren inkomende data
- [ ] Eigenaarcontrole geïmplementeerd waar nodig" \
  --base main
```

Als `gh` niet beschikbaar is, geef de GitHub URL om handmatig een PR aan te maken.

## Schoolvereisten

De school eist minimaal 5 pull requests. Maak een PR voor elke feature:
1. `feat/auth-endpoints` — register + login
2. `feat/note-crud` — note CRUD
3. `feat/learning-path` — learning path CRUD
4. `feat/file-upload` — file upload/download
5. `feat/exception-handling` — GlobalExceptionHandler + tests
