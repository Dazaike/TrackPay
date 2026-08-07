# Phase 2 — History & editing

**Agent role:** implementer  
**Depends on:** Phase 1 acceptance  
**Unblocks:** Phase 3  
**App version when done:** `0.3.0`

---

## Goal

Every shift on the record. Searchable history, totals, filters, session detail, **manual create + full edit + delete** — all free, first-class.

---

## Context / locks

- Sessions already written by timer spine
- Earnings from `EarningsCalculator` + snapshots
- Goal allocations **do not exist yet** — when editing/deleting, leave hooks or no-ops; Phase 3 will attach `recomputeAllocations(sessionId)`
- Notes field on `WorkSession` becomes user-facing
- `source` values: `MANUAL`, `NOTIFICATION`, `EDIT` (and existing ones)

---

## Deliverables

### History tab UI

1. **Summary card**
   - All-time earned (minor → money format)
   - Shift count, total hours (active time)
   - Respect active filters when showing “filtered totals” OR show all-time always + filtered list — pick one, document in UI (“Showing X shifts”)

2. **Search**
   - Query job name + notes (case-insensitive)
   - Debounced

3. **Filters**
   - Job (all / specific)
   - Date range (presets: 7D, 30D, 90D, custom optional)
   - Clear filters control

4. **List**
   - Group by local calendar date (sticky header)
   - Row: job name (fallback “Session”), time range, duration, earned `$`
   - Tap → session detail

5. **Empty states**
   - No sessions ever vs no filter matches

### Session detail

- Job, status, start/end, breaks list, duration breakdown (regular/OT if any), earned
- Notes view
- Actions: Edit, Delete (confirm dialog)

### Manual create

- Entry points: History FAB and/or detail overflow
- Form: job, start, end, optional breaks, notes
- Validation: end > start; no overlap with other active session; if creates COMPLETED only (no manual RUNNING)
- Snapshots copied from job **at save time**
- `source = MANUAL`

### Edit session

- Same form as create; load existing breaks
- May change job → refresh snapshots **only if you explicitly choose “apply current job rates”**; default: **keep original snapshots** (safer for history). Document in UI with a toggle “Recalculate using current job rates”
- Cannot edit into overlapping RUNNING mess; if session was COMPLETED stay COMPLETED
- Editing times/breaks recomputes display earnings automatically
- `source` may become `EDIT` or append edit metadata — keep simple

### Delete

- Cascade breaks
- Confirm

### Repository API

```
observeSessions(query): Flow<List<SessionListItem>>
getSessionDetail(id)
insertCompletedSession(...)
updateSession(...)
deleteSession(id)
totals(filter): SessionTotals
```

List items should include precomputed or efficiently computed `earnedMinor` (compute in mapper using calculator; OK to compute on read for v1).

---

## Domain rules

- Still one RUNNING/PAUSED max; manual create is COMPLETED only
- Edited completed sessions must not restart FGS
- Day-local grouping uses system time zone
- Currency formatting shared with Dashboard (`MoneyText` / `FormatMoney`)

---

## Tests

Unit / repo:

- Filter by job + range
- Search notes
- Edit start/end changes earnedMinor
- Delete removes breaks
- Manual create validation (end before start fails)

---

## Acceptance

- [ ] History shows completed sessions from Phase 1 timer
- [ ] Search + job/date filters work
- [ ] Totals match calculator on known fixtures
- [ ] Manual session appears in list with correct `$`
- [ ] Edit times updates duration and `$`
- [ ] Delete removes session
- [ ] Detail shows breaks and OT split when applicable
- [ ] `VERSION` / `versionName` → `0.3.0`

---

## Out of scope

- Goal allocation recompute (Phase 3 — add TODO hook OK)
- Insights charts (Phase 4)
- Themes (Phase 5)
- Geofence (Phase 6)

---

## Handoff contract → Phase 3

Stable:

- `getSessionDetail`, completed session flows
- Session edit/delete APIs
- Preferred extension point: `onSessionCompleted(sessionId)` / `onSessionMutated(sessionId)` empty callbacks or no-op use case Phase 3 fills for allocations + wallet

---

## Subagent rules

- Do not build Goals product UI
- Do not gate edit behind paywall
- Reuse EarningsCalculator; don’t fork pay math
- assembleDebug + unit tests for edit/filter math
