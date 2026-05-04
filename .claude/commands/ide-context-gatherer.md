Scan my NotePlus Spring Boot project and generate a concise context snapshot I can paste into a chat session.

Read the following and summarize what you find:

1. All files in `src/main/java/org/noteplus/noteplus/controller/` — list which controllers exist and which endpoints they expose
2. All files in `src/main/java/org/noteplus/noteplus/service/` and `service/impl/` — list which service interfaces and implementations exist
3. All files in `src/main/java/org/noteplus/noteplus/dto/` — list which request and response DTOs exist
4. All files in `src/main/java/org/noteplus/noteplus/exception/` — list which exceptions and handlers exist
5. `src/main/resources/data.sql` — does it exist and what data does it seed?
6. All files in `src/test/` — list which test classes exist and roughly how many test methods each has
7. `pom.xml` — list the key dependencies (Spring Security, JWT, Testcontainers, etc.)
8. `.gitignore` — does it exclude `uploads/`, `target/`, `.idea/`?
9. Run `git log --oneline -10` to show the last 10 commits
10. Run `git branch -a` to show all branches

Then output the result in this exact format so I can paste it into a chat:

---

## NotePlus — Current Project Snapshot

**What is implemented:**
- Controllers: [list or "none yet"]
- Services (interfaces): [list or "none yet"]
- Services (implementations): [list or "none yet"]
- DTOs: [list or "none yet"]
- Exception handling: [list or "none yet"]
- data.sql: [exists / missing]
- Test classes: [list with method count or "none yet"]

**Git status:**
- Last 10 commits: [output of git log]
- Branches: [output of git branch]

**What is still missing based on CLAUDE.md:**
- [list any controllers / services / DTOs / tests that CLAUDE.md says are needed but don't exist yet]

**My current question for this session:**
[Checkout those files, and give me back the context, so we can give that context back to the claude session and ask it to generate code based on that context. Be concise but include all relevant details.]

---
