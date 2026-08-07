# Phase briefs — subagent handoff

Each file is a **standalone work order**. A subagent should need only that file + repo root defaults — not the full `PLAN.md` novel.

| Order | File | Name | Depends on |
|------:|------|------|------------|
| 0 | [`00-scaffold.md`](./00-scaffold.md) | Scaffold | — |
| 1 | [`01-timer-spine.md`](./01-timer-spine.md) | Timer spine | Phase 0 |
| 2 | [`02-history-editing.md`](./02-history-editing.md) | History & editing | Phase 1 |
| 3 | [`03-goals.md`](./03-goals.md) | Goals | Phase 2 |
| 4 | [`04-insights-motivation.md`](./04-insights-motivation.md) | Insights & motivation | Phase 3 |
| 5 | [`05-themes.md`](./05-themes.md) | Themes | Phase 4 |
| 6 | [`06-settings-location-polish.md`](./06-settings-location-polish.md) | Settings, location, polish | Phase 5 |

**Global locks (every phase)**

- App: **TrackPay** · package **`com.trackpay.app`**
- Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore
- **No** billing, paywall, widgets, Wear OS, cloud accounts
- All features free
- Money = integer minor units · time = UTC millis
- Version bump on every meaningful ship (`VERSION` + app `versionName`)
- Skip repo-wide format/lint/test storms unless the phase brief says otherwise; prove *your* acceptance checks

**How to run a phase**

1. Confirm previous phase acceptance is met (or you’re starting 0 on empty tree).
2. Implement only this phase’s deliverables.
3. Leave handoff notes at the bottom of the phase file or in commit message if contracts changed.
4. Do not start the next phase in the same agent pass unless explicitly told to chain.
