# TrackPay

your shift, priced by the second

Every generic time tracker treats your job like a timesheet for a boss who doesn’t care. TrackPay treats it like a live stock ticker for the only asset you actually sell — your hours. Clock in. Watch the dollars climb. Park a cut of each shift into goals that fill while you work. No account. No cloud guilt trip. No “Pro” upsell hiding the edit button.

## What it is

Material 3 Android app. Local-first pay timer + savings goals + insights. Inspired by the iOS app *Clocked In*, rebuilt without the nickel-and-diming.

- Live earnings on screen and in the ongoing notification (Android 13+ live status path)
- Unlimited jobs, overtime rules, pause/resume
- Manual session create/edit — yes, free, because forgetting to clock in is a personality trait not a SKU
- Goals that auto-take a % of each shift
- Insights, streaks, weekly challenges, cosmetic themes unlocked by tracked earnings
- Optional geofence nudge when you show up to work

## What it isn’t

- Wear OS (yet)
- Home screen widgets (yet)
- A bank, a payroll system, or another subscription wearing a productivity costume

## Status

**Phase 3 goals complete.** Savings goals with % allocation on clock-out and session edit/delete, templates, pace-to-deadline, Goals tab, dashboard peek.

| | |
|---|---|
| Version | `0.4.0` |
| Package | `com.trackpay.app` |
| Stack | Kotlin · Compose · Material 3 · Room · Hilt · DataStore |
| Min SDK | 26 (live-status polish on 33+) |

**Master plan:** [`PLAN.md`](./PLAN.md)  
**Runbook:** [`EXECUTION.md`](./EXECUTION.md) — order, contracts, smoke gate  
**Execute via:** [`phases/`](./phases/README.md) — one brief per agent

## Build

Requires JDK 17+ and Android SDK 35 (`ANDROID_HOME` / `local.properties` `sdk.dir`).

```bash
./gradlew :app:assembleDebug
```

Or open the repo root in Android Studio (Ladybug+) and run the `app` configuration.

## Repo

```
TrackPay/
  app/              Android application module
  gradle/           version catalog + wrapper
  PLAN.md           product + architecture lock
  EXECUTION.md      build order + contracts + smoke gate
  phases/           subagent work orders (00–06)
  README.md         you are here
  VERSION           semver pointer
```

## Privacy posture

Your shifts and rates stay on device. That’s the product, not a footnote.

---

Clock in or keep guessing what an hour is worth. Your call.
