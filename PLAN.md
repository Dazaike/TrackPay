# TrackPay — Build Plan

**Status:** v1 implemented · phases 0–6 complete · tag suggestion `v1.0.0`  
**Version:** 1.0.0  
**Source reference:** [Clocked In: Budget & Savings](https://apps.apple.com/us/app/clocked-in-budget-savings/id6772886245)  
**Target:** Material 3 Android · Kotlin · Jetpack Compose  
**Repo:** `/home/daz/Scratchpad/TrackPay`
**Execution runbook:** [`EXECUTION.md`](./EXECUTION.md)

---

## Product

Local-first hourly pay timer. Clock in, watch earnings tick live, fund savings goals, read insights. Motivation app, not payroll.

**Monetization:** none. Every feature ships free. No Play Billing, no paywall, no freemium gates, no “Pro.”

**Explicitly out of scope (for now)**

- Wear OS
- Home screen widgets
- Cloud sync / accounts
- Ads
- Auto schedule clock-in (backlog only)

---

## Feature set (all free)

### Timer & jobs

- Unlimited jobs: name, hourly rate, OT rate + threshold, color/icon
- Rate snapshot on session start (history doesn’t rewrite if you edit the job later)
- Clock in / out / pause / resume
- Live per-second earnings on Dashboard
- Manual session create + full session edit (start/end, breaks, notes, job)
- Optional geofence arrival/leave reminders + auto clock (best-effort, not payroll truth)

### History

- Date-grouped sessions, search by job/notes
- All-time totals (money, shifts, hours)
- Filters: job, date range, notes
- Session detail + delete

### Goals

- Unlimited goals: target, deadline, icon/color, % of earnings allocated
- Progress + auto pace (`$ / wk` to hit deadline)
- Templates (Home Fund, Laptop, etc.)
- Allocate on clock-out from net session earnings

### Insights & motivation

- Weekly challenge (auto target from prior week)
- Ranges: 7D / 30D / 90D / 1Y
- Earnings / hours charts
- Average-by-weekday (highlight best day)
- Achievements + streaks

### Themes

- Cosmetic wallet = lifetime tracked earnings (not real cash)
- Unlock/apply palettes (Classic Blue free baseline; Verdant, Sunset, Berry, …)
- No IAP for themes — earn unlocks by working in-app only

### Settings

- Jobs, currency, notifications, location auto-clock
- Analytics off by default (optional later)
- Feedback (mailto or deferred)
- About / privacy (local-first copy)

### Live status (Android 13+)

Android substitute for iOS Live Activities — **not widgets, not Wear**.

| Tier | API | Behavior |
|------|-----|----------|
| Baseline | 26+ | Foreground service + ongoing notification: job, elapsed, live `$`, pause/clock-out actions |
| Android 13+ (API 33) | 33+ | Ongoing notification with high-update cadence while session active; notification permission flow; stable FGS types / poster |
| Progressive | 36+ (Android 16) when available | System **Live Updates** style progress notification if platform supports it — same data pipeline as FGS |

**Hard requirement:** `$` and elapsed must advance without reopening the app (fix the iOS “stuck until open” complaint). Recompute from wall clock + persisted `startAt` / breaks; never trust a UI-only timer.

---

## Information architecture

```
NavigationBar (5)
├── Dashboard   live timer / idle home
├── History     search, filters, sessions
├── Insights    challenge, charts, streaks
├── Goals       progress + templates
└── Settings    jobs, prefs, location, about
```

**Secondary:** onboarding, job editor, goal editor, session editor, theme shop, achievement sheet, permission rationales.

### Dashboard

- **Running:** huge live `$`, `HH:MM:SS`, rate · since time, Clock Out (error), Pause
- **Idle:** Clock In, today/week chips, top goal peek, streak chip
- Job picker when multiple jobs exist

---

## Domain model

```
Job
  id, name, hourlyRateMinor, otRateMinor?, otThresholdMinutes?,
  color, icon, geofence?, createdAt, archived

WorkSession
  id, jobId
  startAt, endAt?
  status: RUNNING | PAUSED | COMPLETED | CANCELLED
  snapshotHourlyRateMinor, snapshotOtRateMinor?, snapshotOtThresholdMinutes?
  notes?, source: MANUAL | GEOFENCE | NOTIFICATION | EDIT

BreakInterval
  id, sessionId, startAt, endAt?

Goal
  id, name, targetMinor, deadline, icon, color
  allocationBps, status: ACTIVE | COMPLETED | ARCHIVED

GoalAllocation
  id, goalId, sessionId, amountMinor, createdAt

Achievement / Streak / WeeklyChallenge / Theme / AppWallet
  (as needed; wallet = sum completed session earnings)
```

**Money:** integer minor units. **Time:** UTC millis, display in system zone.

### Pay math (unit-test these)

- Active time excludes breaks
- OT: per calendar day per job first (document in UI); snapshot thresholds on session
- Earnings derived, not stored as source of truth
- Goal allocations on clock-out; sum of goal bps ≤ 10_000; recompute allocations if session edited
- Wallet += session net on complete

---

## Stack

| Layer | Choice |
|-------|--------|
| Lang / UI | Kotlin, Jetpack Compose, Material 3 |
| Arch | single `app` module first; split only if painful |
| DI | Hilt |
| DB | Room |
| Prefs | DataStore |
| Async | Coroutines · Flow |
| Nav | Navigation Compose |
| Charts | Vico |
| Timer | `TimerForegroundService` + notification actions |
| Live status 13+ | ongoing notif; Live Updates API when compile SDK allows |
| Location | Play Geofencing (optional) |
| Billing | **none** |
| Widgets | **none** |
| Wear | **none** |
| Min SDK | 26 (live-status UX polish targets 33+) |
| Compile / target | 35+ (bump for Live Updates when we implement that path) |

Privacy posture: local-first, no account. Optional analytics later, off by default.

---

## Material 3

- Dark-first, emerald / Verdant default (theme packs override seed)
- Hero money: large tabular figures, brand green
- `NavigationBar` × 5
- Clock Out = error filled; Clock In = primary filled
- Cards: tonal `surface-container`
- Light scheme still required
- Dynamic color optional; **active theme pack wins**

---

## Architecture

```
Compose UI · ViewModels
        ↓
domain use cases (ClockIn/Out, Pause, LiveEarnings, AllocateGoals, EditSession…)
        ↓
Room · DataStore · Geofence · ThemeRepository
        ↓
TimerForegroundService  ← canonical "now" while RUNNING/PAUSED
```

**Session state machine:** `Idle → Running ⇄ Paused → Completed` (single active session).

Notification actions call the same use cases as the UI.

---

## Phases

**Subagent work orders:** [`phases/`](./phases/README.md) · **run order/contracts/smoke:** [`EXECUTION.md`](./EXECUTION.md)

| Phase | Brief | Ship version |
|-----:|-------|--------------|
| 0 | [Scaffold](./phases/00-scaffold.md) | `0.1.0` |
| 1 | [Timer spine](./phases/01-timer-spine.md) | `0.2.0` |
| 2 | [History & editing](./phases/02-history-editing.md) | `0.3.0` |
| 3 | [Goals](./phases/03-goals.md) | `0.4.0` |
| 4 | [Insights & motivation](./phases/04-insights-motivation.md) | `0.5.0` |
| 5 | [Themes](./phases/05-themes.md) | `0.6.0` |
| 6 | [Settings, location & polish](./phases/06-settings-location-polish.md) | `1.0.0` |

Root bullets below are orientation only. **If a brief conflicts with this file, the brief wins** until this file is updated.

### Phase 0 — Scaffold

Compose M3 shell, Hilt, Room, DataStore, 5-tab placeholders. → `phases/00-scaffold.md`

### Phase 1 — Timer spine

Jobs, clock in/out/pause, FGS live `$`, Dashboard hero, pay math tests. → `phases/01-timer-spine.md`

### Phase 2 — History & editing

List/search/filters, manual create, full edit/delete. → `phases/02-history-editing.md`

### Phase 3 — Goals

CRUD, templates, allocate on clock-out/mutate, pace. → `phases/03-goals.md`

### Phase 4 — Insights & motivation

Challenge, charts, weekday averages, streaks, achievements. → `phases/04-insights-motivation.md`

### Phase 5 — Themes

Cosmetic wallet from lifetime earnings, unlock/apply packs. → `phases/05-themes.md`

### Phase 6 — Settings & location & live-status polish

Settings IA, geofence, onboarding, a11y, notif polish, Live Updates if SDK allows. → `phases/06-settings-location-polish.md`

### Deferred

- Wear OS
- Home screen widgets
- Cloud backup/sync
- Schedule-based auto clock-in
- Export (nice later)

---

## Risks

1. **BG accuracy** — persist timestamps; recompute; FGS owns ticker
2. **FGS / notification policy** — lawful service type + Play declaration; don’t abuse media types
3. **Update cadence vs battery** — 1s when interactive; slower when backgrounded if needed, still monotonic `$`
4. **OT definition** — day-based v1, labeled in UI
5. **Goal % > 100%** — reject on save
6. **Edit session** — recompute pay + goal allocations transactionally
7. **Geofence** — reminder-grade only

---

## Testing

- Domain unit tests (money/time/goals/streaks)
- Room in-memory repo tests
- Instrumented smoke: clock in → notification/DB advances without UI
- No billing tests
- No widget tests

---

## Defaults (locked unless you change them)

| Item | Value |
|------|--------|
| App name | TrackPay |
| Package | `com.trackpay.app` |
| Default theme | Verdant dark |
| OT default | after 8h / calendar day / job |
| Goal allocation default | 0% until user sets |
| Min SDK | 26 |
| Live status focus | API 33+ ongoing; richer Live Updates later |
| IAP / Pro | **removed entirely** |
| Widgets | **removed** |
| Wear | **removed** |

---

## v1 “done” line

Ship when: unlimited jobs, live timer + FGS notification that ticks `$` closed, full history + filters + session edit/create, goals with allocate, insights (all ranges), themes wallet, settings, optional geofence, onboarding. No billing. No widgets. No Wear.

**Implemented:** v1.0.0 ships the done line above (settings IA, onboarding, optional geofence suggest-via-notification, a11y polish). No billing, widgets, or Wear.

---

## Changelog (plan)

- **0.1.0-plan** — initial clone plan with Pro/IAP, widgets, Wear deferred
- **0.2.0-plan** — all former Pro features free; strip billing; drop widgets; drop Wear; keep Android 13+ live status
- **0.3.0-plan** — split each phase into standalone subagent briefs under `phases/`
- **0.4.0-plan** — add `EXECUTION.md` runbook (critical path, contracts, smoke gate, agent rules)
