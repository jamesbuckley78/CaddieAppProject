# CaddieAppProject

Mobile app to replace a golf club's manual caddie list. Built by James (BSc Computer Systems student, comfortable in Java/Python, learning JavaScript/TypeScript, develops on Windows).

Full plan and decisions so far: see `docs/PLAN.md`. Read it before starting work.

## Stack
- Mobile: React Native with Expo (TypeScript, Expo Router). One codebase for Android, iOS and web (admin may use a tablet/PC). Test with Expo Go and the Android Studio emulator; iOS builds via EAS Build (no Mac available).
- Backend: Spring Boot (Java 21, Maven) - Web, Data JPA, Security (JWT), Validation, WebSocket, Flyway.
- Database: MySQL 8 in Docker (`docker-compose.yml`). No Redis for now.
- Editors: IntelliJ for `backend/`, VS Code for `mobile/`.

## Layout
- `docs/` - PLAN.md, RULES.md, API.md (keep API.md's status column up to date).
- `backend/` - Spring Boot, base package `com.caddieapp`, one package per feature (auth, caddie, availability, queue, loop, absence, issue, notification, audit, realtime, config, common). Schema changes are new Flyway files in `src/main/resources/db/migration/`; never edit one that has already run.
- `mobile/` - Expo Router app: `app/login.tsx`, `app/(caddie)/`, `app/(admin)/`, plus `components/`, `api/`, `hooks/`, `types/`. Backend calls go through `mobile/api/client.ts`.

## Conventions
- All list-ordering logic lives in `backend/src/main/java/com/caddieapp/queue/QueueService` and is covered by unit tests. Rules are documented in `docs/RULES.md` - if a rule is unclear, ask James rather than guessing.
- One queue in the database; singles and doubles are filtered views of it, never separate stored lists.
- Every admin change to the list (move, promote, demote, edit bag) writes to the audit log.
- Assigning a caddie must run in a transaction with a row lock (two admins may act at once).
- Explain new JavaScript/TypeScript or React Native concepts briefly when introducing them.
