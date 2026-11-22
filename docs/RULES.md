# Queue rules

The rules `QueueService` follows when ordering today's caddie list. Every rule here must have a unit test in `backend/src/test/java/com/caddieapp/queue/QueueServiceTest.java`.

If a rule is missing or unclear, ask the caddie master and write the answer here before coding it.

## Confirmed rules
- There is one queue per day. The singles and doubles boards are filtered views of it.
- Everyone is reset to `OFF` at the end of the day.
- A caddie joins the queue when they press "I'm here" (status `OFF` -> `AVAILABLE`).
- A priority caddie is a single or a double, never a junior (enforced when an admin creates a caddie).

## Caddie master's answers (from docs/PLAN.md)
| # | Question | Answer | Still to confirm before coding |
|---|---|---|---|
| 1 | Do priority caddies always go before everyone else, or only within their grade? | Priority first, then everyone else (non-priority singles and doubles go after). | - |
| 2 | After a loop, does a caddie go to the bottom of today's list, or is their place based on bags carried? | Their place is based on bags: a single counts as one, a double as two. | Bags carried **today** or over the **season**? Fewer bags = higher up? How are ties broken (check-in time?) |
| 3 | Does a double count as one turn or two? | Two. | - |
| 4 | Can a double caddie take a single if no singles are free? Are juniors singles only? | Doubles can take a single, but rarely. Juniors can take singles and doubles. | Is "doubles take singles" the admin's choice on the board, or automatic when the singles list is empty? |
| 5 | What happens on a no-reply? | If a person isn't there, they go up the list as they didn't take a bag. | So no penalty: they keep their bag count, and the next person is called? Do they stay `AVAILABLE` or go to `BREAK`? |
| 6 | Is an admin's manual move a one-off or permanent? | Admin can move caddies at their discretion. | Does a manual move stick for the rest of the day, or get undone by the next bag count change? |
| 7 | Does the club's review system have an API we could link to? | Unknown. | - |
