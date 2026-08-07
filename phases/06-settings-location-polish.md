# Phase 6 — Settings, location & live-status polish

**Agent role:** implementer  
**Depends on:** Phase 5 acceptance  
**Unblocks:** v1 done / release candidate  
**App version when done:** `1.0.0`

---

## Goal

Ship-ready product layer: real Settings IA, optional geofence auto clock, onboarding, empty states, a11y, battery-aware notification ticking, Android 13+ live-status polish (+ Live Updates path if compile SDK supports it). No widgets. No Wear.

---

## Context / locks

- Core product complete through themes
- Geofence is **best-effort reminder / auto clock**, not payroll truth — copy must say so
- Location optional; app fully works without it
- Notification permission optional but prompted
- Analytics remain **off** unless you add a clearly labeled opt-in stub (default false); no requirement to ship PostHog
- Still **no** billing, widgets, Wear

---

## 1. Settings IA

Replace placeholder with grouped list:

**Work**

- Jobs (list/edit — full CRUD UI if still thin)
- Default job
- Overtime defaults help text

**Preferences**

- Currency code (ISO 4217 list or common set) + symbol via `NumberFormat`
- Theme → navigate Themes
- Dark/Light/System override (optional if theme packs already force dark — prefer System + packs)

**Notifications**

- Live session notification toggle (if off, FGS may still need a minimal notif on some API levels — handle legally)
- Open system notification settings

**Location**

- Auto clock-in/out master switch
- Per-job work location picker (map or system picker + radius)
- Explanation copy + “Always” permission rationale

**Support**

- Feedback `mailto:` 
- Privacy policy screen (local-first text; can mirror PLAN posture)
- About + version (`versionName`)

**Advanced**

- Export later stub hidden OR omit
- Reset demo data (debug only)

---

## 2. Onboarding (first launch)

DataStore `onboardingDone`

Steps (3–4 max):

1. Welcome value prop  
2. Create first job (name + rate) — required  
3. Optional: first goal shortcut  
4. Permissions: notifications (33+), location “Not now” allowed  

Land on Dashboard idle ready to clock in.

---

## 3. Geofencing

- Play Services Geofencing API
- Per job: lat, lng, radius meters (default 100–150m), enabled flag
- On ENTER: if idle and auto-on → `ClockIn(jobId, source=GEOFENCE)` or notification “You’re here — Clock in?” **Prefer confirm notification if auto feels risky; implement one behavior and document.** Recommended: **actionable notification** default; hard auto optional secondary toggle.
- On EXIT: if active session for that job → suggest Clock out / auto pause
- Manifest permissions: fine/coarse/background location with rationales
- Graceful absence of Play Services

---

## 4. Live status polish (Android 13+)

| API | Work |
|-----|------|
| 26–32 | Ongoing FGS notif already from Phase 1 — polish actions/layout |
| 33+ | Permission preflight UX; notification runtime request in onboarding + settings |
| 34+ | Correct `foregroundServiceType` + any property declarations for Play |
| 36+ if available in SDK | Wire **Live Updates** / progress-centric notification style using **same** earnings pipeline |

**Battery**

- 1s updates when app visible OR notification high-priority session
- Backoff to 5s when backgrounded > N minutes if needed — `$` still recomputed from wall clock (monotonic, no freeze)
- Never require opening app to refresh totals

---

## 5. Empty states & a11y

- Every tab: sensible empty + CTA
- Content descriptions on Clock In/Out, money hero (announce rounded currency)
- Font scale: hero money doesn’t explode layout (autosize or max lines)
- Contrast check on theme primaries

---

## 6. Hardening

- Process death: active session restores, FGS restarts if RUNNING/PAUSED (boot? optional `BOOT_COMPLETED` rehydrate if session active — nice-to-have)
- Transactional session mutations remain safe with goals/themes wallet derive
- Crash-free smoke path: onboard → clock in → pause → resume → clock out → history → goal funded → insights → theme unlock

---

## Tests

- Currency format for selected code
- Onboarding flag
- Geofence transition handler unit test (fake use case invokes)
- Notification update backoff math (if extracted)
- Permission-less paths don’t crash

---

## Acceptance

- [ ] Settings covers jobs, currency, themes link, notifications, location, about
- [ ] Fresh install onboarding creates a job and reaches Dashboard
- [ ] App usable with all permissions denied
- [ ] Geofence path either auto or suggest clock with clear copy (if Play Services present)
- [ ] API 33+ notification permission flow exists
- [ ] Background `$` still advances without opening UI
- [ ] Empty states + basic a11y on primary actions
- [ ] Smoke path above works
- [ ] `VERSION` / `versionName` → `1.0.0`
- [ ] README updated for run instructions + feature list (dbrand voice OK)
- [ ] No widgets, Wear, billing

---

## Out of scope (still deferred)

- Wear OS
- Home screen widgets  
- Cloud sync  
- Schedule-based auto clock-in  
- CSV export (unless trivial — don’t block 1.0)

---

## Handoff contract → release

- App is v1 feature-complete per root `PLAN.md` done line
- Mark `PLAN.md` status → `v1 implemented` in a small note if you touch it
- Tag suggestion: `v1.0.0`

---

## Subagent rules

- Don’t reintroduce Pro/IAP
- Don’t add widget module “while you’re here”
- Prefer polish + permissions correctness over new product surfaces
- assembleDebug + critical unit tests; instrumented smoke if emulator available
