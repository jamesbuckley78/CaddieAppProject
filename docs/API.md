# API

Base URL in development: `http://localhost:8080/api`

All endpoints except `/auth/login` and `/health` need an `Authorization: Bearer <token>` header.

Status: **planned** means not built yet. Update this table as endpoints are added.

## Phase 1 (MVP)
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/health` | anyone | Check the server is up | built |
| POST | `/auth/login` | anyone | Phone + password -> JWT | planned |
| POST | `/auth/set-password` | caddie | Set password on first login | planned |
| POST | `/admin/caddies` | admin | Create a caddie account | planned |
| GET | `/admin/caddies` | admin | List caddies | planned |
| POST | `/availability/check-in` | caddie | "I'm here" - join today's list | planned |
| POST | `/availability/break` | caddie | Go on break | planned |
| POST | `/availability/back-in` | caddie | Back in after a loop or break | planned |
| GET | `/queue/today` | both | Today's list (filter with `?bag=SINGLE` or `?bag=DOUBLE`) | planned |
| GET | `/queue/me` | caddie | Own position and how many are ahead by bag type | planned |
| POST | `/loops` | admin | Assign a bag to a caddie (row-locked transaction) | planned |
| PATCH | `/loops/{id}` | admin | Change the bag type | planned |

## Real-time
WebSocket (STOMP) at `/ws`. Clients subscribe to `/topic/queue` and get the updated list whenever it changes.
