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

**Plan locked. Phase briefs written for subagents. App code not started.**

| | |
|---|---|
| Version | `0.3.0-plan` |
| Package (planned) | `com.trackpay.app` |
| Stack (planned) | Kotlin · Compose · Material 3 · Room · Hilt |
| Min SDK (planned) | 26 (live-status polish on 33+) |

**Master plan:** [`PLAN.md`](./PLAN.md)  
**Execute via:** [`phases/`](./phases/README.md) — one brief per agent

## Repo

```
TrackPay/
  PLAN.md           product + architecture lock
  phases/           subagent work orders (00–06)
  README.md         you are here
  VERSION           semver pointer
```

When code lands it’ll be a normal Android Gradle tree. Until then: plan hard, ship once.

## Privacy posture

Your shifts and rates stay on device. That’s the product, not a footnote.

---

Clock in or keep guessing what an hour is worth. Your call.
