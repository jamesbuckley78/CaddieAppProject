# API

Base URL in development: `http://localhost:8080/api`

All endpoints except `/auth/login` and `/health` need an `Authorization: Bearer <token>` header.
Errors come back as `{"error": "message"}` with a matching status:

| Status | Meaning |
|---|---|
| 400 | Request is invalid (message says which field) |
| 401 | No token, or the token is expired/cancelled - log in again |
| 403 | Logged in but not allowed (caddie on an admin endpoint, or still on a setup code) |
| 404 | Not found |
| 409 | Conflicts with the current state (e.g. phone number already used, caddie already called) |
| 429 | Too many wrong logins on this phone number - wait 15 minutes |

Status: **planned** means not built yet. Update this table as endpoints are added.

## Accounts and login
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/health` | anyone | Check the server is up | built |
| POST | `/auth/login` | anyone | `{phone, password}` -> `{token, user}`. First login uses the 6-digit setup code (valid 48 h). 5 wrong tries locks the number for 15 min (429) | built |
| POST | `/auth/set-password` | any logged-in user | `{newPassword}` -> new `{token, user}`. Logs out every other device | built |
| GET | `/auth/me` | any logged-in user | Current user (the app checks an admin's saved token with this) | built |
| POST | `/auth/renew` | caddie | A fresh 90-day `{token, user}`; the app calls it on start-up so caddies stay logged in. 403 for admins (their 12 h limit is on purpose) | built |
| POST | `/auth/push-token` | any logged-in user | `{provider: "EXPO" \| "FCM", token}` - where to send push notifications. The token is taken off anyone else who registered it (a phone passed on) | built |
| POST | `/admin/caddies` | admin | `{name, phone, grade, priority, placement?}` -> `{caddie, setupCode}` (201). Juniors can't be priority. `placement`: `TOP_OF_GROUP` (default) or `BOTTOM_OF_GROUP` (copying the paper list) | built |
| GET | `/admin/caddies` | admin | All caddies in club list order, then those who left (`active: false`, `listPosition: 0`) | built |
| POST | `/admin/caddies/{id}/edit` | admin | `{name, phone, grade, priority}`. Changing group (promote / demote / junior to adult) puts them at the top of the new group. Audit logged | built |
| POST | `/admin/caddies/{id}/left` | admin | Left the club: off the club list, logged out, can't log in. Bags and history kept. Audit logged | built |
| POST | `/admin/caddies/{id}/back` | admin | Bring back someone who left: top of their group, old password works again. Audit logged | built |
| GET | `/admin/admins` | admin | The current admins | built |
| POST | `/admin/admins` | admin | `{name, phone}` -> `{admin, setupCode}` (201). Same setup-code login as caddies. Audit logged | built |
| POST | `/admin/admins/{id}/reset-code` | admin | Forgotten password: new 48 h setup code, logs them out. Audit logged | built |
| POST | `/admin/admins/{id}/remove` | admin | They've left: logged out, can't log in (204). Not your own account. Audit logged | built |
| POST | `/admin/caddies/{id}/reset-code` | admin | Forgotten password: new 48 h setup code; old password and logins stop working | built |
| POST | `/admin/caddies/{id}/move` | admin | `{position}` - move on the club list within their group (priority / adults / juniors). Audit logged | built |

## Checking in (caddie)
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| POST | `/availability/check-in` | caddie | "I'm here". Body `{latitude, longitude}`; 400 without a position, 403 outside the club area (`app.check-in`) | built |
| POST | `/availability/status` | caddie | `{status: AVAILABLE \| FOOD \| BACK_LATER, backInMinutes?}` (5-240). Keeps their place. 409 if called or out | built |
| POST | `/availability/check-out` | caddie | "Finished for today". 409 if called or out | built |
| POST | `/availability/on-my-way` | caddie | After being called - shows on the board | built |
| POST | `/availability/back-in` | caddie | After a loop - available again | built |
| GET | `/queue/me` | caddie | My day: status, `backAt`, `lineNumber`, bags this week, club list number, and while called: `calledBagType`, `calledAt`, `callAccepted` | built |
| GET | `/queue/club-list` | both | The club list table: `{countsUpTo, updatesAt, totalBags, days (Mon-Fri), dayTotals, weekTotal, rows}`. Order and figures update together daily at 4pm | built |
| GET | `/history/me?weeks=8` | caddie | My loops and days worked by week: `{totalBags, totalDaysWorked, weeks: [{weekStart, bags, loops, daysWorked, loopRows}]}` | built |

## The board (admin)
Every board action locks the caddie's row (two admins can't call the same caddie), is written to the audit log, and returns the updated board.

| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/admin/board` | admin | Everyone checked in today, in line order: status, line number (null = out), `moved`, `missedCalls`, call details, `currentLoopId`/`currentBagType`/`currentDoubleLoop` while out, `awayToday` names, `openIssues` count, `weekDays` + each entry's `bagsByDay` and `printPosition` for the printed list. `totalBagsToday` is live | built |
| POST | `/admin/board/{caddieId}/call` | admin | `{bagType}` - calls a waiting caddie; they get a push notification + see it on Today. 409 if already called/out | built |
| POST | `/admin/board/{caddieId}/send-out` | admin | They've turned up: records the bag (loop) and marks them out on course. Clears a manual move | built |
| POST | `/admin/board/{caddieId}/no-reply` | admin | Called caddie didn't answer: back to waiting, same place, `missedCalls` + 1 | built |
| POST | `/admin/board/{caddieId}/back-in` | admin | Back from a loop (the caddie can also do it themselves) | built |
| POST | `/admin/board/{caddieId}/move` | admin | `{lineNumber}` - manual place in today's line, holds until their next bag. `{lineNumber: null}` undoes it | built |
| POST | `/admin/loops/{loopId}/bag-type` | admin | `{bagType}` - correct a bag (was really a single/double) | built |
| POST | `/admin/loops/{loopId}/double-loop` | admin | `{doubleLoop: true/false}` - round twice on the same bag; a marked loop counts twice. Audit logged, returns the board | built |
| POST | `/admin/loops` | admin | `{caddieId, date, bagType}` - a bag entered by hand (201), today or earlier. Counts like any bag, never shows as out on course. Audit logged | built |
| POST | `/admin/club-list/recalculate` | admin | Run the daily club list update now (normally the 4pm job does it) | built |

## Not coming in
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/absences/mine` | caddie | `{earliestDate, requests}` - earliest date with 24 h notice, and my requests from today on | built |
| POST | `/absences` | caddie | `{date, reason?}` -> 201 pending. 400 if under 24 h notice, 409 if already asked about that day | built |
| DELETE | `/absences/{id}` | caddie | Withdraw a request the admin hasn't answered yet | built |
| GET | `/admin/absences` | admin | Inbox: `{pending, recentlyDecided}` (last 14 days) | built |
| POST | `/admin/absences/{id}/decision` | admin | `{approve: true/false}`. Audit logged | built |

## Issue reports
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| POST | `/issues` | caddie | `{type: SLOW_PLAY \| RANGER \| GOLFER_PROBLEM \| GOLFER_GOING_IN \| MISSING_ITEM, hole?: 1-18, message?}` (201) | built |
| GET | `/admin/issues` | admin | Open issues, oldest first: `[{id, caddieName, type, hole, message, createdAt}]` | built |
| GET | `/admin/issues/summary?date=2026-11-25` | admin | Every report in the Mon-Sun week containing `date`, or all time without it: `{weekStart, total, byType, byCaddie (most reports first), issues (newest first, max 200, with resolved / resolvedBy / resolvedAt)}` | built |
| POST | `/admin/issues/{id}/resolve` | admin | Mark dealt with (204); records who and when. 409 if already resolved | built |

## Attendance (admin)
| Method | Path | Who | Purpose | Status |
|---|---|---|---|---|
| GET | `/admin/attendance?date=2026-11-25` | admin | The Mon-Sun week containing `date` (default this week): `{weekStart, days, dayTotals, caddiesIn, rows: [{caddieId, name, grade, priority, checkedInAt (7, null = not in), daysWorked}]}` | built |

## Accounts
- No public sign-up. The first admin is created on start-up from `app.bootstrap-admin.*` in `application.yml` (dev default: phone `0870000000`, password `admin1234`) if no admin exists. They must set a new password on first login.
- Phone numbers are stored with spaces, dashes and brackets removed, so `087 123 4567` and `0871234567` are the same login.
- Caddie logins last 90 days and are renewed each time the app starts (`/auth/renew`); admin logins last 12 hours. Changing a password or an admin reset cancels every older login (`users.token_version`).

## Real-time
For now the app refreshes on a timer (`mobile/hooks/useLiveData.ts`: board every 10 s, caddie Today screen every 10 s) and whenever a screen comes into view. Calls also go out as Expo push notifications.
Not planned: WebSockets. At ~200 phones polling every 10 s (~20 requests/s) polling is cheap, and the urgent case ("You're up!") already goes out as a push notification. Revisit only if 10 s is too slow in practice; any live ping must not include names, because caddies can't see who is checked in.
