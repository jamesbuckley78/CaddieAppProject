# Queue rules

The rules `QueueService` follows when ordering caddies. Every rule here must have a unit test in `backend/src/test/java/com/caddieapp/queue/QueueServiceTest.java`.

If a rule is missing or unclear, ask the caddie master and write the answer here before coding it.

## Two lists

### 1. Club list (everyone can see it)
The full list of all caddies, saved in `caddies.list_position`. It is recalculated **once a day at 4pm** (Europe/Dublin, `app.club-list.daily-update-time`) and does not move automatically at any other time. If the server was off at 4pm, it catches up when it starts.

Order - three groups, always in this order:
- **Priority caddies (the top 50)**, then **other adults**, then **juniors** (e.g. positions 90-97). Juniors are never priority. A caddie never moves out of their group: juniors stay together at the bottom and only move around each other.

Within each group:
1. **Priority caddies always come first**, whatever their bag count. A non-priority caddie on 0 bags is still below every priority caddie.
2. Within each group (priority, then non-priority): **fewest bags carried this week first.** A single counts as 1 bag, a double as 2. Singles, doubles and juniors are **not** split in this list.
3. **Ties keep the previous night's order.** No alphabetical order and no check-in times. If a tie needs sorting out, the admin moves people by hand.

Bag counts:
- Counts add up **Monday to Sunday**, then everyone starts the new week on 0. The order carries over into the new week.
- Each 4pm update counts every bag entered up to that moment. Bags entered after 4pm count from the next day's update - except on Sunday, where bags after 4pm fall into a week that has already been closed off, so they never affect the order.
- A caddie who doesn't come in keeps their count (e.g. still 2 bags on Wednesday). Because everyone else's count goes up, they move up relative to them.

Starting the list:
- The first list is the paper list: the admin adds caddies in paper-list order (the "add at the bottom" option).
- A new caddie added later goes to the **top of their group** (priority, other adults or juniors) with 0 bags. When copying the paper list, each goes to the **bottom of their group** - so an adult added that way still goes above the juniors.
- A bag carried by someone not yet on the list can be entered by the admin by hand, and they can be moved to the right spot.

### 2. Today's line (worked out live, nothing reordered in the database)
Who gets called next today. Only caddies who checked in today appear, ordered by:
0. The same three groups as the club list: priority, other adults, then **juniors at the bottom** of the board, after every adult.
1. Priority first.
2. Fewest bags this week, **including today's** - so someone who just carried a bag drops below everyone in their group with fewer bags.
3. Ties: club list position.

- A caddie sees **their own number in line** ("You're 5th in line") counted among checked-in caddies who aren't out on course, plus today's club-wide bag total. They don't see who else is checked in.
- The admin board shows everyone checked in, in this order, as two filtered views: **Singles** (single and junior caddies) and **Doubles** (double and junior caddies). Juniors can take singles and doubles, so they appear in both with a J tag. A double caddie taking a single is the admin's choice (Q4).

## Checking in and statuses
- **"I'm here"** needs the phone to be inside the club area set in `application.yml` (`app.check-in`). The server checks the distance, so a modified app can't skip it. Location is read only at that moment.
- Once checked in, a caddie can be: **Available**, **Getting food**, **Will be back** (with their own time estimate, e.g. 15 min - the admin sees "back ~10:45"), or **Finished for today** (checks out). Later: **Out on course**, set when the admin assigns a bag.
- Getting food / Will be back **keep their place**. The status only tells the admin they may not answer straight away; the admin can still call them.

## Calling and no-reply (next feature)
- When the admin calls a caddie they get a notification.
- If they don't reply, it's the admin's call whether to try again or call the next person. The non-replier **keeps their place** ("a missed opportunity") and is next in line for the following call. No automatic timer.

## Manual moves by the admin (next feature)
- Used when someone comes back from another job, or to fix a mistake (e.g. wrong bag type entered).
- A move **holds for that day until the caddie gets a bag** (or the next 4pm update), then the normal rules apply to them again. Every move goes in the audit log.

## Club list screen and daily total
- Shown as a table: position, name with one coloured tag (**P** priority, otherwise **S** single, **D** double, **J** junior), bags on each day Mon-Fri, and the week total (weekend included - that's what orders the list). A **Total** row at the bottom shows the whole club's bags each day and for the week.
- The order, the bag figures and the club-wide total ("120 bags today", a double counts as 2) **all update together at 4pm**, so the numbers always match the order. Before 4pm they show up to yesterday, so on Monday morning the table still shows last week until Monday 4pm. Each update saves a snapshot (`club_list_snapshots`) recording which bags it counted.
- The total is at the top of the club list, not on the caddie's home screen. The home screen shows their club list number on its own (e.g. "73").
- The admin board keeps a live total for the caddie master.

## Still open
| # | Question | Answer |
|---|---|---|
| 7 | Does the club's review system have an API we could link to? | Unknown |
