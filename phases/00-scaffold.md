# Phase 0 — Scaffold

**Agent role:** implementer  
**Depends on:** empty repo (plan docs only)  
**Unblocks:** Phase 1  
**App version when done:** `0.1.0`

---

## Goal

Bootable Android app shell. Material 3, Hilt, Room, DataStore, 5-tab nav with placeholder screens. Zero product logic.

---

## Context

- Repo root has `PLAN.md`, `README.md`, `VERSION`, `phases/`.
- No Gradle project yet — you create it.
- Package: `com.trackpay.app`
- Min SDK 26, compile/target 35
- Single `app` module only

---

## Deliverables

1. **Gradle project**
   - Kotlin DSL preferred (`build.gradle.kts`, `settings.gradle.kts`)
   - Version catalog optional but nice
   - Application id `com.trackpay.app`
   - `versionName` `0.1.0` / `versionCode` 1
   - Compose + Material 3 + Navigation Compose + Hilt + Room + DataStore dependencies wired
   - `VERSION` file → `0.1.0`

2. **Application class**
   - `@HiltAndroidApp` `TrackPayApp`

3. **Main activity**
   - `ComponentActivity` + `setContent { TrackPayTheme { TrackPayAppShell() } }`
   - Edge-to-edge OK

4. **Theme**
   - Dark-first **Verdant** seed (emerald primary on near-black)
   - Light color scheme also defined
   - Typography baseline M3
   - File under `ui/theme/`

5. **Navigation shell**
   - Bottom `NavigationBar` with 5 destinations:
     - Dashboard
     - History
     - Insights
     - Goals
     - Settings
   - Each destination: simple placeholder scaffold (`Text` title + short “Phase N” stub is fine)
   - State hoisted so tab selection survives recomposition

6. **DI / data stubs**
   - Hilt modules package exists
   - Room `TrackPayDatabase` empty or with a no-op/version-1 empty schema that compiles
   - DataStore `<preferences>` provider injectable
   - **No** entities required beyond what Room needs to build (empty DB OK)

7. **Manifest**
   - App label TrackPay
   - No FGS/location permissions yet (Phase 1+)

8. **README**
   - One-line “how to open in Android Studio / assemble” if missing
   - Do not rewrite marketing voice wholesale

---

## Suggested tree

```
app/src/main/java/com/trackpay/app/
  TrackPayApp.kt
  MainActivity.kt
  ui/shell/TrackPayAppShell.kt
  ui/shell/TopLevelDestination.kt
  ui/theme/Color.kt Theme.kt Type.kt
  ui/dashboard/DashboardPlaceholder.kt
  ui/history/HistoryPlaceholder.kt
  ui/insights/InsightsPlaceholder.kt
  ui/goals/GoalsPlaceholder.kt
  ui/settings/SettingsPlaceholder.kt
  di/AppModule.kt
  data/local/TrackPayDatabase.kt
  data/local/PreferencesDataSource.kt
```

---

## Acceptance

- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] App launches to Dashboard placeholder with 5 working tabs
- [ ] Dark Verdant theme visible (not plain purple baseline only)
- [ ] Hilt injection works (e.g. inject DataStore or DB into a tiny VM/placeholder)
- [ ] No billing, widget, or Wear modules/code
- [ ] `VERSION` is `0.1.0`

---

## Out of scope

- Jobs, sessions, timer, notifications
- Real screen UI beyond placeholders
- Tests beyond “it assembles” (optional smoke only)

---

## Handoff contract → Phase 1

Phase 1 will add:

- `Job` / `WorkSession` / `BreakInterval` entities
- `TimerForegroundService`
- Dashboard real UI

Keep nav destinations stable (`dashboard|history|insights|goals|settings` routes).

---

## Subagent rules

- No app code outside this phase’s goal
- No drive-by refactors of plan markdown except version pointers if needed
- Skip full lint/test suites; assemble is the proof
