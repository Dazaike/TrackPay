# Phase 5 — Themes

**Agent role:** implementer  
**Depends on:** Phase 4 acceptance  
**Unblocks:** Phase 6  
**App version when done:** `0.6.0`

---

## Goal

Cosmetic theme shop. Wallet funded by **lifetime tracked earnings**. Unlock with wallet balance, apply Material 3 palettes. Not real money. No IAP.

---

## Context / locks

- Wallet balance = sum of calculator earnings over all COMPLETED sessions (derive; optional cache table updated on session mutate)
- Spending wallet on themes is **cosmetic ledger only** — does not reduce goal savings or real cash
- Decide and document one model:

  **Model A (recommended):** unlock is permanent flag; “price” is minimum lifetime earned threshold (Clocked In screenshot style cart is flavor — treat price as unlock gate against lifetime earned, no balance debit)

  **Model B:** debit wallet on purchase; balance = lifetimeEarned − sum(themePrices bought)

  Implement **Model A** unless Model B already fits easier — match marketing line “earn hours to fund unlocks” with threshold unlocks + optional display wallet = lifetime earned.

- Active theme id in DataStore
- Active theme **wins over** dynamic color

---

## Catalog (seed)

| id | name | unlockMinor | notes |
|----|------|-------------|-------|
| classic_blue | Classic Blue | 0 | always owned |
| verdant | Verdant | 2500 ($25) | default active for new installs if free… **Verdant is default UI today at $25 in reference** — keep **Verdant free default active**, Classic Blue also free; paid gates start Sunset |
| sunset | Sunset | 5000 | |
| berry | Berry | 10000 | |
| lagoon | Lagoon | 25000 | |
| amethyst | Amethyst | 50000 | |
| ember | Ember | 100000 | |

Adjust copy to match: free defaults = Classic Blue + Verdant; others threshold-locked.

Each theme defines M3 seed or full light/dark ColorSchemes (primary, secondary, tertiary, surface family).

---

## Domain

```
ThemePack
  id, name, unlockMinor, light: ColorScheme tokens, dark: ColorScheme tokens
  // tokens can be serializable color longs

UserThemeState
  ownedIds: Set<String>   // or derive owned = unlockMinor <= lifetimeEarned
  activeId: String
```

If Model A derive ownership: `lifetimeEarned >= unlockMinor` (plus always free ids).

Use cases:

- `ObserveWallet` → lifetimeEarnedMinor  
- `ObserveThemes` → list with locked/owned/active  
- `ApplyTheme(id)` — only if owned  
- `UnlockTheme(id)` — no-op if Model A derive; if Model B debit  

---

## UI

### Entry

- Settings row “Themes” **or** Insights/Settings; also reachable from Settings tab primary list
- Dedicated Themes screen (not required as 6th tab)

### Themes screen

- Wallet card: `$X,XXX.XX` + disclaimer: “Earn hours to unlock cosmetic themes. Doesn’t affect real money or goals.”
- List rows: color dot, name, price or “Tap to apply”, Active chip, lock/cart affordance
- Tap locked: snackbar how much more to earn
- Tap owned: apply immediately; theme recomposes app-wide

### Theme application

- `TrackPayTheme` reads `activeId` Flow  
- Swap schemes; status bar icons contrast update  

---

## Tests

- Free themes always apply
- Locked theme reject apply
- Ownership flips true when lifetime earned crosses threshold (Model A)
- Active id persists (DataStore test or repo test)

---

## Acceptance

- [ ] Themes screen shows wallet = lifetime earnings
- [ ] Default Verdant (or Classic Blue) active on fresh install
- [ ] Crossing unlock threshold enables apply without reinstall
- [ ] Applied theme changes primary/accent across tabs
- [ ] Disclaimer visible
- [ ] No Play Billing code
- [ ] `VERSION` / `versionName` → `0.6.0`

---

## Out of scope

- Selling themes for real currency
- Per-screen wallpapers
- User custom hex builder
- Widgets themed (no widgets)

---

## Handoff contract → Phase 6

- `ThemeRepository.observeActiveScheme()` stable
- Settings will link to Themes route

---

## Subagent rules

- Don’t add IAP “shortcuts” to unlock
- Don’t debit goal savings
- Keep catalog in code
- assembleDebug + unit tests for unlock rules
