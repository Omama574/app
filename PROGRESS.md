# Build log — Echo + Notifii (two apps)

This file is the hand-off between sessions. The same file lives in both repos:
- **Echo** (deleted-messages app, ads + one-time "Remove ads") → `github.com/Omama574/app` (folder `echo/`)
- **Notifii** (offline "ladder" notification app, one-time Pro) → `github.com/Omama574/notifii`, branch `ladder-v1`

If you're a new session: read this file top to bottom, then continue from **Next steps**.

## Decisions (locked with the user)
- Two apps. Echo first (mass-market download magnet), Notifii second (offline, privacy brand).
- Notifii: **no INTERNET permission, no account — non-negotiable.** Echo may use internet **only for ads**; messages never leave the phone.
- Money: Echo = in-feed native ads (AdMob) + one-time "Remove ads". Notifii = free core + one-time Pro. **No subscriptions, no interstitials, no app-open ads.**
- Payments: Google Play Billing (required by Play for digital goods). No server; purchases restored locally with `queryPurchasesAsync`.
- Auto-reply: later (not v1).
- UX is the moat: one mental model per app, value before asks, just-in-time permissions.

## Mental models
**Echo:** "Your chats, plus the messages people deleted." Two tabs: **Chats** (every saved conversation) and **Deleted** (the hero feed). Chat view looks like a messenger; deleted bubbles are marked, edited messages show the original.

**Notifii:** every app/person sits on one rung of a ladder — **Siren** (rings through silent/DND until handled) · **Ring** (normal; default) · **Later** (held, delivered in a digest) · **Hide** (never shown). **Everything is always kept in the Vault**, so nothing is ever lost. Exceptions are made from a real notification ("Upwork is Later, except 'invited you' → Siren"), previewed against the Vault, and every action explains **Why?**. Night window: Ring behaves like Later; Siren still rings. Tabs: **Now · Ladder · Vault**. Free calms your phone; Pro (Siren + exceptions beyond 1) makes sure you never miss.

## Design rules we follow (from research)
- Value before any ask; one activation moment; aim for value inside 60 s; ask permissions at the moment they're needed ([Userpilot](https://userpilot.com/blog/contextual-experience/), [Eleken](https://eleken.co/blog-posts/mobile-app-onboarding-best-practices)).
- Material 3: colour roles, 8 dp grid, 48 dp touch targets, 4.5:1 text contrast, dark theme, type scale, themed motion, restraint ([M3 in Compose](https://developer.android.com/jetpack/compose/themes/material3), [Androidify](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html)).
- Native ads: in the feed, visually distinct, labelled "Ad", AdChoices, nothing overlapping ([AdMob native policy](https://support.google.com/admob/answer/6329638), [playbook](https://admob.google.ca/home/resources/native-ads-playbook)).
- Paywall: one product, stripped back, benefit clarity ([RevenueCat](https://www.revenuecat.com/blog/growth/paywall-conversion-boosters.md)).
- Play requires targetSdk 36 for new apps since 31 Aug 2026 ([Android Developers](https://developer.android.com/google/play/requirements/target-sdk)).

## Toolchain (known-good on the dev machine)
AGP 8.13.2 · Gradle 8.13 · Kotlin 2.0.21 · KSP 2.0.21-1.0.27 · Compose BOM 2025.02.00 · Room 2.6.1 · compileSdk/targetSdk 36 · minSdk 26 · JDK 17.

## Status
- [x] Research + decisions
- [x] Echo: skeleton builds
- [x] Echo: capture engine + deletion detection (+ 7 unit tests, passing)
- [x] Echo: UI (onboarding, chats, chat, deleted, search, settings)
- [x] Echo: ads (UMP consent + native) + Remove-ads billing (compiles; real purchase needs Play Console)
- [ ] Echo: emulator smoke test + screenshots
- [ ] Notifii: rebuild on ladder model (+ unit tests)
- [ ] Notifii: UI (onboarding, now, ladder, vault, why/exception sheets, siren screen, paywall)
- [ ] Notifii: billing with INTERNET stripped from merged manifest
- [ ] Notifii: emulator smoke test + screenshots

## What we need from the user (blocking only for release, not for building)
1. Final app names + package IDs (working: `app.echo.messages`, `com.notifii.app`). Package IDs can't change after first upload.
2. Google Play Console account ($25 one-time) + payments profile.
3. AdMob account → real App ID + native ad unit ID (code uses Google's **test IDs** now).
4. In Play Console create in-app products: Echo `remove_ads`, Notifii `notifii_pro` (one-time, non-consumable).
5. A hosted privacy-policy URL for each app (GitHub Pages works; drafts are in `PRIVACY.md`).

## How to continue
```bash
./gradlew :app:testDebugUnitTest   # logic tests
./gradlew :app:assembleDebug       # build
```

## Next steps
(updated as work progresses)
