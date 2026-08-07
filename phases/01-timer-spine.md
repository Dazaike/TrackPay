# Phase 1 — Timer spine

**Agent role:** implementer  
**Depends on:** Phase 0 acceptance  
**Unblocks:** Phase 2  
**App version when done:** `0.2.0`

---

## Goal

Real clock-in engine. Unlimited jobs, live earnings on Dashboard, foreground service notification that keeps ticking `$` when the app is backgrounded. Domain math unit-tested.

---

## Context / locks

- Package `com.trackpay.app`, M3 Verdant, 5-tab shell exists
- **No Pro gates** — unlimited jobs from day one
- Money: `Long` minor units. Time: UTC epoch millis
- Single active session globally
- State machine: `Idle → Running ⇄ Paused → Completed`
- OT rule v1: **per calendar day per job**, default threshold **8 hours** (480 minutes)
- Snapshot rates onto session at clock-in
- Earnings always **derived** from timestamps + snapshots + breaks — never a stored running total as source of truth

---

## Domain

### Entities (Room)

```
Job
  id: String (uuid)
  name: String
  hourlyRateMinor: Long
  otRateMinor: Long?          // null = no OT
  otThresholdMinutes: Int?    // null = default 480 if otRate set; or require explicit
  colorArgb: Int
  iconKey: String
  archived: Boolean
  createdAt: Long

WorkSession
  id: String
  jobId: String
  startAt: Long
  endAt: Long?
  status: RUNNING | PAUSED | COMPLETED | CANCELLED
  snapshotHourlyRateMinor: Long
  snapshotOtRateMinor: Long?
  snapshotOtThresholdMinutes: Int?
  notes: String?              // may stay null until Phase 2 UI
  source: MANUAL | NOTIFICATION | …  // enum string

BreakInterval
  id: String
  sessionId: String
  startAt: Long
  endAt: Long?                // null = currently paused
```

### Pure functions (test these)

`EarningsCalculator` (name flexible):

- Input: session snapshots, breaks, `now` (or endAt), job day boundaries in system zone
- Output: `activeMinutes`, `regularMinutes`, `otMinutes`, `earnedMinor`
- Pause excluded from active time
- OT: minutes above threshold for that job’s calendar day (include other completed sessions same job/day when computing remaining regular — document behavior; simplest v1: **threshold applies to this session’s active minutes only** if multi-session day merge is hard — prefer day-merge if straightforward)

`LiveEarnings`: same calculator with `now = clock.now()`.

### Use cases

- `UpsertJob` / `ListJobs` / `ArchiveJob`
- `ClockIn(jobId)` — fails if session already active; copies rate snapshot; starts FGS
- `PauseSession` / `ResumeSession` — break row open/close
- `ClockOut` — endAt, status COMPLETED, stop FGS
- Ensure only one RUNNING/PAUSED session

---

## Service & live status

`TimerForegroundService`

- Started on clock-in / resume; stopped on clock-out
- Ongoing notification: job name, elapsed `HH:MM:SS`, earned `$`, actions Pause|Resume, Clock Out
- Actions → same use cases as UI (not duplicated logic)
- Recompute from DB timestamps every tick — **do not** increment a float in memory as truth
- Cadence: ~1s while session active (throttle later in Phase 6 if needed)
- API 33+: request `POST_NOTIFICATIONS` before starting; graceful if denied (session still runs, no notif)
- Manifest: FGS permission + **lawful** `foregroundServiceType` (prefer documented special-use / dataSync per current Play rules — do **not** fake `mediaPlayback`). Add property/declaration placeholders as required.
- Notification channel: e.g. `session_live`

---

## UI

### Dashboard (replace placeholder)

**Idle**

- Primary **Clock In**
- If multiple jobs: job selector (default last-used or first)
- Chips: today earned, week earned (compute from completed sessions + active live)
- Empty state if no jobs → CTA to create job

**Running / Paused**

- Huge live money (`MoneyText`, tabular)
- Elapsed with green dot (paused: tertiary/amber treatment)
- Subline: `$rate/hr · since <time>`
- Buttons: **Clock Out** (error filled), Pause / Resume
- Collect Flow that emits every second while active (UI) **and** trust service for BG

### Jobs (minimal for this phase)

- Reachable from Dashboard overflow or Settings stub link
- List + add/edit: name, hourly rate, optional OT rate + threshold, color/icon presets
- No geofence fields yet

### Settings tab

- Can remain mostly placeholder; link “Jobs” is enough

---

## Data layer

- DAOs + `SessionRepository` / `JobRepository`
- `observeActiveSession(): Flow<ActiveSession?>`
- DataStore: `lastJobId` optional

---

## Tests (required for this phase)

Unit tests (JVM):

- earnings with no breaks
- pause excluded
- OT above threshold
- snapshot rates used not current job rates
- clock-in rejected when active session exists

No full instrumented suite required; manual or one instrumented smoke nice-to-have: start session → notif shows.

---

## Acceptance

- [ ] Create job with rate; clock in; Dashboard `$` and elapsed tick every second
- [ ] Pause freezes earnings; resume continues correctly after process-friendly recompute
- [ ] Clock out writes COMPLETED session; service stops; notification dismissed
- [ ] Kill app / background: notification still shows advancing `$` (or advances on next tick from wall clock)
- [ ] Unlimited jobs; switch job only when idle
- [ ] Domain unit tests green
- [ ] `VERSION` / `versionName` → `0.2.0`
- [ ] Still no billing/widgets/Wear

---

## Out of scope

- History list UI (Phase 2)
- Session edit/manual create (Phase 2)
- Goals / allocations (Phase 3)
- Charts, streaks (Phase 4)
- Theme shop (Phase 5)
- Geofence (Phase 6)
- Android 16 Live Updates API (Phase 6)

---

## Handoff contract → Phase 2

Stable:

- Session + break schema
- `EarningsCalculator` API
- `SessionRepository` completed session queries
- Status enum values

Phase 2 will list/filter/edit sessions and add notes UI.

---

## Subagent rules

- Do not implement History/Goals product UI
- Do not add Play Billing
- Prefer extending Phase 0 packages over new modules
- Prove calculator with unit tests; assembleDebug must pass
