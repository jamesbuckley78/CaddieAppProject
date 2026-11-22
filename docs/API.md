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
| POST | `/availability/check-in` | caddie | "I'm here" - join today's list | planned |
| POST | `/availability/break` | caddie | Go on break | planned |
| POST | `/availability/back-in` | caddie | Back in after a loop or break | planned |
| GET | `/queue/today` | both | Today's list (filter with `?bag=SINGLE` or `?bag=DOUBLE`) | planned |
| GET | `/queue/me` | caddie | Own position and how many are ahead by bag type | planned |
| POST | `/loops` | admin | Assign a bag to a caddie (row-locked transaction) | planned |
| PATCH | `/loops/{id}` | admin | Change the bag type | planned |

## Accounts
- No public sign-up. The first admin is created on start-up from `app.bootstrap-admin.*` in `application.yml` (dev default: phone `0870000000`, password `admin1234`) if no admin exists. They must set a new password on first login.
- Phone numbers are stored with spaces, dashes and brackets removed, so `087 123 4567` and `0871234567` are the same login.
- Tokens last 90 days (`app.jwt.expiry-days`).

## Real-time
WebSocket (STOMP) at `/ws`. Clients subscribe to `/topic/queue` and get the updated list whenever it changes.
