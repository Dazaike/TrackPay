# Phase 4 — Insights & motivation

**Agent role:** implementer  
**Depends on:** Phase 3 acceptance  
**Unblocks:** Phase 5  
**App version when done:** `0.5.0`

---

## Goal

Insights tab that answers “what is my time worth?” Weekly challenge, range charts, weekday averages, streaks, achievements.

---

## Context / locks

- Completed sessions with calculator earnings are source data
- Ranges: **7D / 30D / 90D / 1Y** (rolling, local TZ)
- Charts: **Vico** (preferred) or Compose chart lib already in project — add Vico if missing
- No paywall on advanced ranges — all free
- Motivation data local-only

---

## Features

### 1. Weekly challenge

- Week = locale Monday–Sunday or Sunday–Saturday — **use ISO Monday-start** unless platform default is clearly better; document
- `targetMinor` = sum earned in **previous** completed week; if 0, fallback baseline e.g. `hourly * 20h` from primary job or `$100` floor — pick simple rule and test it
- `earnedMinor` = sum this week (completed + optionally live active session slice)
- Card: title, days left, `$earned / $target`, progress bar
- Persist nothing heavy if pure derived; cache target at week start in DataStore if you want stability when user deletes old sessions mid-week

### 2. Range selector + charts

- Segmented: 7D / 30D / 90D / 1Y
- Toggle: **Earnings** | **Hours**
- Bar chart by day (7D/30D) or week buckets (90D/1Y) — choose readable bucketing
- Header total for range

### 3. Average by weekday

- Mean earnings (or hours) for Sun…Sat over selected range or last 90D — **use selected range**
- Highlight max day in primary color; others muted

### 4. Streaks

```
StreakState
  currentDays: Int
  bestDays: Int
  lastActiveLocalDate: LocalDate?
```

- Active day = any COMPLETED session with active minutes > 0 that local day
- Current streak: consecutive days ending today or yesterday (standard habit rule: allow today incomplete if yesterday counted)
- Persist best in DataStore or recompute always (recompute fine)

Dashboard idle chip: `🔥 N day streak` when current ≥ 1

### 5. Achievements

Define a small fixed catalog (code, not CMS):

| id | title | rule |
|----|-------|------|
| first_shift | First clock-out | ≥1 completed session |
| early_bird | Start before 7 local | session start hour < 7 |
| week_warrior | 5 active days in a week | |
| ot_hero | Session with OT minutes > 0 | |
| goal_starter | Create a goal | |
| goal_funded | First allocation > 0 | |
| streak_7 | 7-day streak | |
| earned_1k | Lifetime earned ≥ 100000 minors ($1,000) | |

- Unlock once; store `achievement_id + unlockedAt`
- UI: horizontal strip or section on Insights; unlock sheet/snackbar on transition (best-effort)

---

## Data

- `InsightsRepository` / use cases pure aggregation over sessions
- Prefer one query of sessions in range → aggregate in domain (simpler testing)
- Achievement evaluator runs on session complete + on Insights open

---

## UI — Insights tab

Replace placeholder with:

1. Weekly challenge card  
2. Range chips  
3. Chart card (earnings/hours)  
4. Average by weekday card  
5. Streak summary  
6. Achievements grid/strip  

Loading/empty: “Clock a shift to unlock insights.”

---

## Tests

- Week challenge target = previous week sum
- Range totals fixture
- Weekday average + highlight index
- Streak across midnight edge with fixed Clock
- Achievement unlock idempotent

---

## Acceptance

- [ ] Insights populate from real sessions
- [ ] All four ranges switch chart data
- [ ] Earnings/Hours toggle works
- [ ] Weekday highlight correct on fixture
- [ ] Weekly challenge progresses after sessions
- [ ] Streak increments with daily completes
- [ ] At least 5 achievements can unlock and stay unlocked
- [ ] `VERSION` / `versionName` → `0.5.0`

---

## Out of scope

- Theme shop (Phase 5)
- Geofence / onboarding polish (Phase 6)
- Export CSV
- Server leaderboards

---

## Handoff contract → Phase 5

- Lifetime earned query available (`sum all completed`) for wallet
- Achievement unlock bus optional for theme unlocks later — not required

---

## Subagent rules

- No widgets
- No blocking features as Pro
- Keep aggregations pure + unit tested
- assembleDebug must pass
