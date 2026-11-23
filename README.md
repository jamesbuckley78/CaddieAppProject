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
One package per feature: `auth`, `caddie`, `availability`, `queue`, `loop`, `absence`, `issue`, `notification`, `audit`, plus `config` and `common`. Each has a `package-info.java` saying what belongs in it.
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
- Setup codes work for 48 hours. Forgot a password, or code expired? Admin taps the caddie on the **Caddies** tab, then **New code**.
- Five wrong passwords on one phone number locks it for 15 minutes.
- Admin logins last 12 hours; caddies stay logged in for 90 days.

## Using it
- **Caddie - Today:** "I'm here", then Available / Getting food / Will be back / Finished for today. When called: "You're up!" -> **On my way**. After a loop: **Back in**.
- **Caddie - Club list, Not coming in, History** tabs: the club table, absence requests (24 h notice), and their own loops and days worked.
- **Admin - Board:** tap a name for its buttons: **Call - single/double**, **Move to number**, then **Sent out** / **No reply**, and for someone out on course **Back in** / **Was a double/single** / **Double loop** (counts the bag twice). On a phone, switch between Singles / Doubles / Out on course; each shows 15 at a time.
- **Admin - Board:** also shows a red banner when caddies have reported issues, and (in a browser) a **Print preview** button that opens the printable list with its own Print button. If the server can't be reached after a reload, it shows the last saved list so it can still be printed.
- **Admin - Caddies:** add caddies; tap one to move them on the club list (within their group), edit details / grade / priority, add a bag by hand, give a new setup code or mark them as left. Leavers are listed at the bottom with **Bring back**.
- **Admin - Inbox:** issue reports to mark resolved, "not coming in" requests to approve or reject, and the **Issue history** by week or all time: totals per type, which caddies report the most, and who resolved each one.
- **Admin - Attendance:** who came in each day of a week, with check-in times and totals.
- **Admin - Admins:** add another caddie master, give one a new setup code (forgotten password) or remove one who has left.
- **Caddie - Report issue:** slow play, ranger, golfer problem, golfer wants to go in, missing item, with an optional hole and note.

## Push notifications
The backend sends an Expo push notification when a caddie is called. Push only works in a real installed build on a phone - not in Expo Go, the emulator or the web, where the caddie still sees the call on their Today screen within 10 seconds. To try it: `npx eas init` (adds the project id to `app.json`), then an EAS development build on an Android phone.

## Demo data (local testing only)
40 made-up caddies (phone numbers starting 0899) with bags this week and a mix of statuses today, so the board and club list have something to show. Run from the project folder in PowerShell:
```bash
Get-Content scripts\demo-data.sql | docker exec -i caddie-mysql mysql -ucaddie -pchange-me caddie
```
Running it again refreshes it for the current week. To remove it:
```bash
Get-Content scripts\demo-data-remove.sql | docker exec -i caddie-mysql mysql -ucaddie -pchange-me caddie
```
Demo caddies can't log in. Never run this against the real club database.

## GPS check-in area
"I'm here" only works within a circle around the club, set in `backend/src/main/resources/application.yml` under `app.check-in` (not changeable from the app):
1. In Google Maps, right-click the caddie shack and click the coordinates to copy them.
2. Put them in `latitude` / `longitude`, pick a `radius-meters` (400 m covers most clubhouses).
3. Set `require-location` to `true`. It's `false` for now so you can test from home.

## Forgotten admin password
Another admin gives them a new setup code on the **Admins** tab. If the only admin is locked out, the password has to be replaced in the `users` table with a new BCrypt hash (and `must_change_password = 1`), since only the hash is stored.
