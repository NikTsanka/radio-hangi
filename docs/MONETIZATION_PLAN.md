# Monetization Plan — Radio Hangi (Free + Pro)

> **Resume keyword: `MONETIZE`** — when the user says this, read this file and continue
> implementation from the agreed phase (start = Phase 1). Status updated 2026-06-03. No code
> has been written yet; the user is preparing the AdMob / Play Console side first.

## Decisions (locked with the user)

- **Distribution model:** **Single app + Pro in-app purchase** (NOT two separate listings).
  One free Play listing, ad-supported by default; a one-time IAP unlocks Pro and removes ads.
- **Price:** **$2.99** one-time, non-consumable INAPP product. Use Play's per-country localized
  pricing (Georgia ~₾5–7; US/EU $2.99/€2.99).
- **Pro perks (all four chosen):**
  1. Remove ads.
  2. Exclusive themes (extra accent palettes / color schemes; non-pro sees locked options).
  3. Unlimited favorites (free version capped, e.g. `FREE_FAVORITES_LIMIT = 10`).
  4. Sleep-timer presets + car mode (extra presets 90/120/custom; simplified big-button screen).
- **Ads:** Google AdMob **interstitial** (video) shown on **every 5th station change**, with a
  **minimum 2–3 min gap** between ads. Free only; no-op when Pro.

## Architecture (single-app, runtime-gated by `proUnlocked`)

1. **`BillingRepository`** (Play Billing Library, `billing-ktx`)
   - Product id: `pro_unlock` (non-consumable INAPP).
   - Exposes `proUnlocked: StateFlow<Boolean>` (queryPurchases on start = restore), `launchPurchase(activity)`,
     handles acknowledgement. App-scoped singleton → add to `AppContainer`.
2. **`AdManager`** (app-scoped, AdMob)
   - `recordStationChange()` increments a counter; on every 5th + min 2–3 min gap, emits a
     `pendingShow` event on a `SharedFlow`.
   - `MainActivity` collects the event and shows the interstitial (needs an Activity).
   - No-op entirely when `proUnlocked == true`.
   - **UMP consent** (GDPR/CCPA) before ad init. Preload next interstitial after each show.
   - Hook points: `RadioViewModel.playStation` and `WorldViewModel.playStation` →
     `adManager.recordStationChange()`.
3. **Pro perks wiring**
   - No ads → gate in `AdManager` by `proUnlocked`.
   - Unlimited favorites → add `AppConfig.FREE_FAVORITES_LIMIT`; `FavoritesRepository` enforces cap
     for non-pro; upgrade prompt on overflow.
   - Exclusive themes → accent/palette picker; lock non-default for non-pro (tap → upgrade).
   - Sleep presets / car mode → extra `SleepTimerOption`s + a simplified car-mode screen, Pro-gated.
4. **Upgrade UI** — a "Go Pro" screen/dialog (perks + price + Buy → billing; "Pro ✓" when owned),
   plus upgrade prompts at each gated feature.
5. **Gradle / deps** — add `play-services-ads`, `billing-ktx`, `user-messaging-platform`.
   AdMob App ID + ad-unit IDs come from `local.properties` → `BuildConfig` (real IDs NOT committed).
   Use Google's **test ad unit IDs** + billing **license testers** during development.

## Implementation phases (build/test one at a time)

- **Phase 1 — Billing + Pro entitlement + Upgrade screen.** Just the `proUnlocked` flag + purchase
  flow + a "Go Pro" UI. Test via license testers / static test responses. (**START HERE.**)
- **Phase 2 — Ads.** AdMob + UMP consent; every-5th-station interstitial; gated by `!proUnlocked`.
- **Phase 3 — Pro perks.** Unlimited favorites (+ free cap), exclusive themes, sleep presets, car mode.

## User's out-of-code tasks (Console side — user is preparing these)

1. AdMob account → app + interstitial ad unit → real IDs.
2. Play Console → create IAP product `pro_unlock` at $2.99 + localized pricing.
3. Ads declaration, Data Safety form, content rating, target API 35.
4. License testers / internal testing track (real ads + billing can't be fully tested on the
   emulator with real IDs — dev uses test IDs).

## Notes on the current codebase (for the implementer)

- Single module `app`, package `com.canka.dev.radiohangi`, MVVM + Compose, AGP 9.2.1, minSdk 24,
  targetSdk 36. App-scoped singletons live in `data/AppContainer.kt`.
- `FavoritesRepository` (DataStore `wr_favs`) — add the free cap here.
- `RadioViewModel.playStation` / `WorldViewModel.playStation` — the station-change ad hook points.
- Theme: `ui/theme/` (gold accent + System/Light/Dark `ThemeMode` toggle hoisted in `MainActivity`).
- `MainActivity` already hosts the Compose tree + splash; it's the place to collect ad-show events.
