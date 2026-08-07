# Phase 3 — Goals

**Agent role:** implementer  
**Depends on:** Phase 2 acceptance  
**Unblocks:** Phase 4  
**App version when done:** `0.4.0`

---

## Goal

Savings goals funded by shifts. CRUD, templates, % allocation on clock-out and on session mutate, pace-to-deadline, Goals tab UI. Unlimited, free.

---

## Context / locks

- Completed sessions + edit/delete exist
- Wire **Phase 2 mutation hooks**: any complete / edit / delete of a session recomputes that session’s allocations
- Allocation currency = session **net earned minor** from calculator
- `allocationBps`: 0–10_000 per goal; **sum of active goals’ bps must be ≤ 10_000** on save
- Default new goal: `allocationBps = 0` until user sets
- Money minor units; deadlines at local date end or noon UTC — pick one, be consistent (`LocalDate` at start of day local preferred)

---

## Domain

### Entities

```
Goal
  id: String
  name: String
  targetMinor: Long
  deadlineEpochDay: Long      // or deadlineAt millis — be consistent
  iconKey: String
  colorArgb: Int
  allocationBps: Int          // 2500 = 25%
  status: ACTIVE | COMPLETED | ARCHIVED
  createdAt: Long
  sortOrder: Int?

GoalAllocation
  id: String
  goalId: String
  sessionId: String
  amountMinor: Long
  createdAt: Long
```

### Derived

- `savedMinor(goal) = sum(allocations)`
- `progress = saved / target` capped 1f
- `remaining = max(0, target - saved)`
- `weeksLeft = max(1/7 week, weeks until deadline)` — if overdue, pace uses 1 week remainder or show “Overdue”
- `pacePerWeekMinor = remaining / weeksLeft`

### Use cases

- `UpsertGoal` (validate bps sum)
- `ArchiveGoal` / `CompleteGoal` (auto-complete when saved ≥ target on recompute)
- `ListGoalTemplates` → materialize goal
- `AllocateSession(sessionId)`  
  - Delete existing rows for session  
  - If session not COMPLETED, no rows  
  - For each ACTIVE goal with bps > 0: `floor(earned * bps / 10000)`  
  - Remainder cents: largest remainder method or leave unallocated (document)
- `RecomputeSession(sessionId)` — used by clock-out, edit, delete (delete → remove allocations)
- `RecomputeAll` — migration/dev

### Clock-out integration

Phase 1 `ClockOut` must call `AllocateSession` after COMPLETED write (transaction).

Phase 2 edit/delete must call recompute/remove.

---

## UI — Goals tab

1. **Header card**
   - Total saved toward active goals
   - Combined target + overall progress bar
   - “X% of target”

2. **Goal list**
   - Icon, name, deadline label
   - `$saved of $target`, %
   - Linear progress in goal color
   - Subline: `Save $X/wk` pace

3. **Templates grid** (“Add another”)
   - Examples: Emergency Fund, Vacation, New Car, Home Fund, New Laptop
   - Prefill name/icon/target/horizon; user confirms

4. **Goal editor**
   - Name, target money, deadline date, icon/color, allocation % slider/field
   - Live warning if total % across goals > 100%
   - Archive / delete

5. **Dashboard peek (light)**
   - Idle Dashboard: top active goal mini progress (if any)
   - Don’t rebuild entire Dashboard

### Settings

- Optional link to Goals; not required

---

## Templates (seed)

| Name | Icon key | Default target | Default horizon |
|------|----------|----------------|-----------------|
| Emergency Fund | shield | 300000 ($3,000) | 6 months |
| Vacation | flight | 250000 | 4 months |
| New Car | directions_car | 600000 | 12 months |
| Home Fund | home | 5000000 | 48 months |
| New Laptop | laptop | 200000 | 4 months |

Targets are suggestions; currency-agnostic minors assume 2-decimal currency.

---

## Tests

- bps sum > 10000 rejected
- allocate 50/50 splits earned
- recompute after edit changes amounts
- delete session removes allocations
- pace math with fixed clock
- auto-complete when threshold crossed

---

## Acceptance

- [ ] Create goal with %; clock out session → allocation rows + saved increases
- [ ] Multiple goals split by bps
- [ ] Edit session earnings → allocations update
- [ ] Delete session → savings drop
- [ ] Templates create editable goals
- [ ] Goals tab matches summary/list/pace UX
- [ ] Dashboard shows goal peek when data exists
- [ ] `VERSION` / `versionName` → `0.4.0`

---

## Out of scope

- Insights charts / weekly challenge (Phase 4)
- Theme wallet spend (Phase 5) — wallet may equal lifetime earnings later; don’t spend here
- Geofence (Phase 6)

---

## Handoff contract → Phase 4

Stable:

- Allocation table + `savedMinor` queries by date range (join sessions)
- Active goals flow
- Session completed pipeline includes allocate

Phase 4 aggregates **session earnings** (not only goal savings) for charts.

---

## Subagent rules

- Don’t invent cloud sync for goals
- Don’t block goals behind paywall
- Keep allocation pure and tested
- assembleDebug + unit tests required
