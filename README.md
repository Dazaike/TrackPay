# TrackPay

your shift, priced by the second

Every generic time tracker treats your job like a timesheet for a boss who doesn’t care. TrackPay treats it like a live stock ticker for the only asset you actually sell — your hours. Clock in. Watch the dollars climb. Park a cut of each shift into goals that fill while you work. No account. No cloud guilt trip. No “Pro” upsell hiding the edit button.

## What it is

Material 3 Android app. Local-first pay timer + savings goals + insights. Inspired by the iOS app *Clocked In*, rebuilt without the nickel-and-diming.

- Live earnings on screen and in the ongoing notification (Android 13+ live status path)
- Unlimited jobs with overtime rules, color/icon, pause/resume
- Manual session create/edit — free, because forgetting to clock in is a personality trait not a SKU
- Goals that auto-take a % of each shift, with templates and pace
- Insights: ranges, charts, weekday averages, streaks, achievements
- Cosmetic themes unlocked by lifetime tracked earnings (no IAP)
- Currency preference (common ISO codes)
- First-launch onboarding (job required; goal + permissions optional)
- Optional geofence arrive/leave **suggestions** via notification (not payroll truth; works fully without location)
- Settings: jobs, currency, themes, notifications, location master switch, feedback mailto, privacy, about

## What it isn’t

- Wear OS
- Home screen widgets
- Cloud sync / accounts
- Billing, IAP, or a “Pro” tier
- A bank or payroll system

## Status

**v1 implemented.** Phase 6 settings, onboarding, optional geofence polish shipped.

| | |
|---|---|
| Version | `1.0.0` (versionCode `7`) |
| Package | `com.trackpay.app` |
| Stack | Kotlin · Compose · Material 3 · Room · Hilt · DataStore |
| Min SDK | 26 (live-status polish on 33+) |
| Target / compile SDK | 35 |

**Master plan:** [`PLAN.md`](./PLAN.md)  
**Runbook:** [`EXECUTION.md`](./EXECUTION.md) — order, contracts, smoke gate  
**Phase briefs:** [`phases/`](./phases/README.md)

## Build & run

Requires **JDK 17+** and **Android SDK 35** (`ANDROID_HOME` or `local.properties` `sdk.dir`).

```bash
# Debug APK
./gradlew :app:assembleDebug

# Unit tests (domain math, etc.)
./gradlew :app:testDebugUnitTest

# Install on a connected device / emulator
./gradlew :app:installDebug
```

Or open the repo root in Android Studio (Ladybug+) and run the `app` configuration.

### Fresh install path

1. Launch → onboarding welcome  
2. Create first job (name + hourly rate)  
3. Optional goal template  
4. Optional notification / location permissions (“Not now” allowed)  
5. Land on Dashboard idle → Clock in  

### Permissions

| Permission | Required? | Why |
|---|---|---|
| Notifications (33+) | Optional | Live session FGS notification |
| Fine/coarse location | Optional | Geofence suggest-via-notification |
| Background location | Only if you enable geo later | Sustained geofences |

App remains usable with all optional permissions denied.

## Feature list (v1.0.0)

| Area | Highlights |
|---|---|
| Dashboard | Live `$`, elapsed, clock in/out/pause, today/week chips, goal peek, streak |
| Jobs | Unlimited CRUD, OT rate/threshold, colors/icons, optional lat/lng/radius |
| History | Search, job/range filters, manual create, full edit/delete |
| Goals | Targets, deadlines, % allocation, templates, pace |
| Insights | Challenge, charts, weekday averages, streaks, achievements |
| Themes | Wallet = lifetime earnings; unlock/apply packs; no IAP |
| Settings | Currency, themes link, live notif toggle, geo master + disclaimer, feedback, privacy, about |
| Onboarding | 4 steps; job required |
| Live status | FGS ongoing notification; `$` advances without opening UI |
| Location | Play Services geofencing when available; graceful no-op otherwise |

## Repo layout

```
TrackPay/
  app/              Android application module
  gradle/           version catalog + wrapper
  PLAN.md           product + architecture lock
  EXECUTION.md      build order + contracts + smoke gate
  phases/           subagent work orders (00–06)
  README.md         you are here
  VERSION           semver pointer (1.0.0)
```

## Privacy posture

Your shifts and rates stay on device. That’s the product, not a footnote. Location and notifications are opt-in. Analytics stay off unless a future labeled opt-in lands (not in v1).

---

Clock in or keep guessing what an hour is worth. Your call.
