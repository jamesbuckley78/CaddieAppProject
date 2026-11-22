# Caddie list app - plan

## Problem
The club runs its caddie order from a manual list. People skip, nobody knows exactly who is present, and the caddie master can't quickly see who is next for a single or a double.

## Who's on the list
- About 200 caddies and up to 5 admins (caddie masters).
- Each caddie has a grade: **single**, **double** or **junior**.
- The top 50 are **priority** caddies. A priority caddie is a single or a double.

## Features
### Caddie
- Logs in once and stays logged in. No public sign-up: an admin creates the account (name, phone, grade, priority) and the caddie sets a password on first login.
- **"I'm here – available"** button joins today's list. It then changes to "Go on break" while waiting and "Back in" after a loop.
  - Later: the first check-in of the day requires scanning a QR code in the caddie shack (changes daily) or being inside the club's GPS area, so nobody checks in from home.
- Home screen: live list, own position, and how many are ahead of them by bag type (e.g. "4 ahead, 1 a double").
- Notification when it's their turn (push first, SMS fallback).
- Can say they're not coming in, at least 24 hours ahead, with or without a reason. The admin approves or rejects it.
- Report issues: slow play / people walking, request a ranger, golfer problem, golfer wants to go in, missing item.
- History of their loops and days worked.

### Admin (caddie master)
- **Board with two tables side by side: Singles and Doubles.** Both are filtered views of the same queue. Show position, name, priority badge, time waiting and an Assign button. Juniors go in the singles table with a "J" tag, or in a small third table (to decide).
- Assign a bag (single/double) and edit the bag type afterwards.
- Edit the list: add a name, drag to reorder, promote or demote (e.g. someone back from another job).
- See who is available, out on a loop or on break, live.
- Absence requests inbox.
- Issues inbox.
- Attendance as total, list or calendar view.

## Status model
`OFF -> AVAILABLE -> CALLED -> OUT -> (Back in) AVAILABLE`, plus `BREAK`. Everyone is reset to OFF at the end of the day.

## Failure handling
- **Called caddie doesn't reply within N minutes:** alert the admin, notify the next 2–3 people ("X isn't answering, you may be up"), then skip or drop that caddie per the rules.
- **Push doesn't arrive:** fall back to SMS (Twilio).
- **Server or wifi down:** the admin app keeps the latest list locally and has a print/export button, so paper works as a backup.

## Data model
| Table | Key fields |
|---|---|
| users | name, phone, role (CADDIE/ADMIN), password hash, push token |
| caddies | user_id, grade (SINGLE/DOUBLE/JUNIOR), is_priority |
| attendance | caddie_id, date, checked_in_at, checked_out_at |
| queue_entries | date, caddie_id, position, status |
| loops | caddie_id, date, bag_type, tee_time, called_at, accepted_at, assigned_by |
| absences | caddie_id, date, reason (nullable), submitted_at, approved |
| issue_reports | caddie_id, type, hole, message, status |
| audit_log | admin_id, action, before_state, after_state, created_at |

## Open questions - ask the caddie master (record answers in docs/RULES.md)
1. Do priority caddies always go before everyone else, or only within their grade? Priority first, then everyone else(singles and doubles that are non priority go after)
2. After a loop, does a caddie go to the bottom of today's list, or is their place based on bags carried (today or season)? Their place is based on the bag, so a single is one, a double is 2
3. Does a double count as one turn or two? 2
4. Can a double caddie take a single if no singles are free? Are juniors singles only? Doubles can but rarely and juniors can take singles and doubles
5. What happens on a no-reply: lose place, drop X places, or skip once? if a person is not there, they go up the list as they did not take a bag
6. Is an admin's manual move a one-off or permanent? Admin can move caddies at their dsicretion
7. Does the club's review system have an API we could link to? unknown

## Build phases
1. **MVP:** docker-compose MySQL, Spring Boot skeleton, login (JWT), admin creates caddies, "I'm here" status button, QueueService with tests, singles/doubles board, assign bag, push notification.
2. **Admin tools:** manual moves with audit log, absence requests, attendance calendar.
3. **Extras:** QR/GPS check-in, issue reports, SMS fallback and escalation job, review system link, EAS builds for iOS/Android.

## Repo layout
```
CaddieAppProject/
├── CLAUDE.md
├── docker-compose.yml
├── .env.example
├── docs/        PLAN.md, RULES.md, API.md
├── backend/     Spring Boot: config, auth, caddie, availability, queue, loop,
│                absence, issue, notification, audit, realtime; Flyway in resources/db/migration
└── mobile/      Expo: app/login.tsx, app/(caddie)/..., app/(admin)/...,
                 components/, api/, hooks/, types/
```

## Next step
Done: skeleton, login, GPS "I'm here" check-in with statuses, the nightly club list, today's line and the admin Singles/Doubles board (rules in docs/RULES.md).
Next: assigning bags (call -> out on course -> back in, row-locked, audit logged), manual moves, and the notification when a caddie is called. Then the security fixes before real caddies use it.
