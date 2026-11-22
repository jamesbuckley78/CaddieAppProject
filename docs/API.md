# API

Base URL in development: `http://localhost:8080/api`

All endpoints except `/auth/login` and `/health` need an `Authorization: Bearer <token>` header.
Errors come back as `{"error": "message"}` with a matching status:

| Status | Meaning |
|---|---|
| 400 | Request is invalid (message says which field) |
| 401 | No token, or the token is expired/invalid - log in again |
| 403 | Logged in but not allowed (caddie on an admin endpoint, or still on a setup code) |
| 404 | Not found |
| 409 | Conflicts with existing data (e.g. phone number already used) |

Status: **planned** means not built yet. Update this table as endpoints are added.

## Phase 1 (MVP)
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/health` | anyone | Check the server is up | built |
| POST | `/auth/login` | anyone | `{phone, password}` -> `{token, user}`. First login uses the 6-digit setup code as the password | built |
| POST | `/auth/set-password` | any logged-in user | `{newPassword}` -> new `{token, user}`. The only thing a user on a setup code can do | built |
| GET | `/auth/me` | any logged-in user | Current user (the app checks its saved token with this) | built |
| POST | `/admin/caddies` | admin | `{name, phone, grade, priority}` -> `{caddie, setupCode}` (201). Juniors can't be priority | built |
| GET | `/admin/caddies` | admin | List caddies, sorted by name | built |
| POST | `/admin/caddies/{id}/reset-code` | admin | Forgotten password: new setup code, old password stops working | built |
| POST | `/admin/caddies` `placement` | admin | Optional `TOP_OF_GROUP` (default, new caddie) or `BOTTOM_OF_GROUP` (copying the paper list) | built |
| POST | `/availability/check-in` | caddie | "I'm here". Body `{latitude, longitude}`; 400 without a position, 403 outside the club area (`app.check-in`). Returns my day | built |
| POST | `/availability/status` | caddie | `{status: AVAILABLE \| FOOD \| BACK_LATER, backInMinutes?}` (5-240, needed for BACK_LATER). Keeps their place. 409 if out on course | built |
| POST | `/availability/check-out` | caddie | "Finished for today" - leaves today's line | built |
| GET | `/queue/me` | caddie | My day: checked in?, status, `backAt`, `lineNumber` ("5th in line"), bags this week, club list number | built |
| GET | `/queue/club-list` | both | The club list table: `{countsUpTo, updatesAt, totalBags, days (Mon-Fri), dayTotals, weekTotal, rows: [{position, name, grade, priority, bagsByDay, weekTotal}]}`. Order and figures update together daily at 4pm (`app.club-list.daily-update-time`) | built |
| GET | `/admin/board` | admin | Everyone checked in today, in line order, with status and line number (null = out on course). `totalBagsToday` is live | built |
| POST | `/admin/club-list/recalculate` | admin | Run the daily club list update now with this week's bags (normally the 4pm job does it) | built |
| POST | `/loops` | admin | Assign a bag to a caddie (row-locked transaction) | planned |
| PATCH | `/loops/{id}` | admin | Change the bag type | planned |
| POST | `/admin/queue/{caddieId}/move` | admin | Manual move, holds until the caddie's next bag (audit logged) | planned |

## Accounts
- No public sign-up. The first admin is created on start-up from `app.bootstrap-admin.*` in `application.yml` (dev default: phone `0870000000`, password `admin1234`) if no admin exists. They must set a new password on first login.
- Phone numbers are stored with spaces, dashes and brackets removed, so `087 123 4567` and `0871234567` are the same login.
- Tokens last 90 days (`app.jwt.expiry-days`).

## Real-time
For now the app refreshes on a timer (`mobile/hooks/useLiveData.ts`: board every 10 s, caddie screen every 15 s) and whenever a screen comes into view.
Planned: WebSocket (STOMP) at `/ws` sending a content-free "changed" ping on `/topic/queue`, so screens refetch straight away. The ping must not include names, because caddies can't see who is checked in.
