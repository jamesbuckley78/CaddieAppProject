# CaddieAppProject

An app to track when caddies are available to work, replacing the golf club's manual caddie list.

## Where things are
| Folder | What's in it | Open with |
|---|---|---|
| `docs/` | `PLAN.md` (the plan), `RULES.md` (queue rules + open questions), `API.md` (endpoints) | anything |
| `backend/` | Spring Boot API (Java 21, Maven) | IntelliJ |
| `mobile/` | Expo / React Native app (TypeScript) for caddies and admins | VS Code |
| `docker-compose.yml` | Local MySQL database | Docker Desktop |

### Backend (`backend/src/main/java/com/caddieapp/`)
One package per feature: `auth`, `caddie`, `availability`, `queue`, `loop`, `absence`, `issue`, `notification`, `audit`, `realtime`, plus `config` and `common`. Each has a `package-info.java` saying what belongs in it.
Database changes go in `backend/src/main/resources/db/migration/` as new `V2__...sql`, `V3__...sql` files.

### Mobile (`mobile/`)
- `app/` - screens. The file path is the route: `app/login.tsx`, `app/(caddie)/...`, `app/(admin)/...`
- `components/` - reusable UI pieces
- `api/` - all calls to the backend
- `hooks/` - reusable state/logic (`useSomething`)
- `types/` - shared TypeScript types

## Running it locally
1. **Database:** copy `.env.example` to `.env`, then
   ```bash
   docker compose up -d
   ```
2. **Backend:** open `backend/` in IntelliJ (it picks up `pom.xml`), run `CaddieAppApplication`. Check http://localhost:8080/api/health.
3. **Mobile:** needs Node.js LTS. Copy `mobile/.env.example` to `mobile/.env`, then from `mobile/`:
   ```bash
   npm install
   npx expo install --fix
   npx expo start
   ```
   Press `a` for the Android emulator, `w` for web, or scan the QR code with Expo Go.

## Logging in (development)
- The first admin is created automatically when the backend starts with no admin in the database: phone **087 000 0000**, password **admin1234**. You'll be asked to choose a new password straight away.
- As admin, open **Caddies**, add a caddie, and note the 6-digit setup code shown.
- Log out, then log in with the caddie's phone number and that code. They choose a password and land on their **Today** screen.
- Forgot a password? Admin taps **New code** next to the caddie.
- Caddies tap **I'm here** on the Today tab. The admin sees them on the **Board** tab; caddies see their own number, today's bag total and the **Club list** tab.

## GPS check-in area
"I'm here" only works within a circle around the club, set in `backend/src/main/resources/application.yml` under `app.check-in` (not changeable from the app):
1. In Google Maps, right-click the caddie shack and click the coordinates to copy them.
2. Put them in `latitude` / `longitude`, pick a `radius-meters` (400 m covers most clubhouses).
3. Set `require-location` to `true`. It's `false` for now so you can test from home.

- Locked yourself out of the admin account? Clear the users and restart the backend (it recreates the default admin):
  ```bash
  docker exec caddie-mysql mysql -ucaddie -pchange-me caddie -e "DELETE FROM caddies; DELETE FROM users;"
  ```
