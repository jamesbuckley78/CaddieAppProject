# Queue rules

The rules `QueueService` follows when ordering today's caddie list. Every rule here must have a unit test in `backend/src/test/java/com/caddieapp/queue/QueueServiceTest.java`.

If a rule is missing or unclear, ask the caddie master and write the answer here before coding it.

## Confirmed rules
- There is one queue per day. The singles and doubles boards are filtered views of it.
- Everyone is reset to `OFF` at the end of the day.
- A caddie joins the queue when they press "I'm here" (status `OFF` -> `AVAILABLE`).

## Waiting on the caddie master
| # | Question | Answer |
|---|---|---|
| 1 | Do priority caddies always go before everyone else, or only within their grade? | |
| 2 | After a loop, does a caddie go to the bottom of today's list, or is their place based on bags carried (today or season)? | |
| 3 | Does a double count as one turn or two? | |
| 4 | Can a double caddie take a single if no singles are free? Are juniors singles only? | |
| 5 | What happens on a no-reply: lose place, drop X places, or skip once? | |
| 6 | Is an admin's manual move a one-off or permanent? | |
| 7 | Does the club's review system have an API we could link to? | |
