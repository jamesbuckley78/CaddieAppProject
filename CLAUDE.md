# CaddieAppProject

Mobile app to replace a golf club's manual caddie list. Built by James (BSc Computer Systems student, comfortable in Java/Python, learning JavaScript/TypeScript, develops on Windows).

Full plan and decisions so far: see `docs/PLAN.md`. Read it before starting work.

## Stack
- Mobile: React Native with Expo (TypeScript, Expo Router). One codebase for Android, iOS and web (admin may use a tablet/PC). Test with Expo Go and the Android Studio emulator; iOS builds via EAS Build (no Mac available).
- Backend: Spring Boot (Java 21, Maven) - Web, Data JPA, Security (JWT), Validation, WebSocket, Flyway.
- Database: MySQL 8 in Docker (`docker-compose.yml`). No Redis for now.
- Editors: IntelliJ for `backend/`, VS Code for `mobile/`.

## Conventions
- All list-ordering logic lives in `backend/.../queue/QueueService` and is covered by unit tests. Rules are documented in `docs/RULES.md` - if a rule is unclear, ask James rather than guessing.
- One queue in the database; singles and doubles are filtered views of it, never separate stored lists.
- Every admin change to the list (move, promote, demote, edit bag) writes to the audit log.
- Assigning a caddie must run in a transaction with a row lock (two admins may act at once).
- Explain new JavaScript/TypeScript or React Native concepts briefly when introducing them.
