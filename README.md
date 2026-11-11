# Echo — see messages after they're deleted

Android app (Kotlin, Jetpack Compose, Material 3). Echo keeps a private, on-device copy of messages that arrive as notifications from chat apps (WhatsApp, Telegram, Instagram, Signal, Messenger, SMS and more), so a message that gets deleted, or edited, can still be read.

- **Chats** — every saved conversation, messenger-style.
- **Deleted** — the messages people took back, with what they said.
- **Search** — across everything saved.
- Optional alert the moment someone deletes a message.

Money: one in-feed native ad (AdMob, with Google UMP consent) and a one-time **Remove ads** purchase (Google Play Billing). No subscriptions, no interstitials.

Privacy: messages never leave the device and are excluded from cloud backup. Internet is used only by the ads SDK. See [PRIVACY.md](PRIVACY.md).

## Build
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
Requires JDK 17 and Android SDK 36. Current status and next steps: [PROGRESS.md](PROGRESS.md).

## Try it on an emulator without a chat app
Debug builds also save notifications posted by the shell, so you can simulate a chat:
```bash
adb shell cmd notification allow_listener app.echo.messages/app.echo.messages.capture.CaptureService
adb shell cmd notification post -S messaging --conversation "Alex" --message "Alex:Are you coming tonight?" chat1 "x"
```
