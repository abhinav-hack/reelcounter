# Reel Counter

A lightweight native Android app that counts scrolling activity in Instagram,
YouTube, and TikTok.

## Legal documents

- [Privacy Policy](./PRIVACY_POLICY.md)
- [Terms and Conditions](./TERMS_AND_CONDITIONS.md)

## How it works

- The app uses an Android Accessibility Service to receive scroll events only
  from the three supported app package names.
- A 750 ms debounce prevents one physical swipe from being counted several
  times, matching the short transition window used by reel viewers.
- Counts are stored locally in `SharedPreferences`, keyed by `yyyy-MM-dd`.
  Reading today's key naturally starts a new zeroed count each day while
  preserving previous dates for history.
- An optional draggable floating bubble shows the live total while a supported
  app is in the foreground.
- The home-screen widget shows today's count and is refreshed after every
  increment.

## Running

Open this folder in Android Studio and run the `app` configuration on an
Android 8.0+ device. On first launch:

1. Enable **Reel Counter scrolling detector** under Android Accessibility
   settings.
2. Allow **Display over other apps** if the live overlay is wanted.
3. Add the Reel Counter widget from the launcher widget picker.

The counter is deliberately local-only. No account, network connection, or
usage data upload is required.

## Play Store release preparation

The Play Store release should be uploaded as an Android App Bundle (`.aab`).
Create or obtain an upload keystore locally, copy
[`keystore.properties.example`](./keystore.properties.example) to
`keystore.properties`, and replace its placeholders. The keystore and
properties file are excluded from source control. The release Gradle task reads
these values from that file (or from matching user Gradle properties) and never
embeds the passwords in the APK. Use forward slashes for the Windows keystore
path in the properties file.

Build the signed bundle with:

```text
gradlew.bat :app:bundleRelease
```

Before publishing, complete Play Console declarations for the Accessibility
Service and “Display over other apps” permission. These permissions are core
to counting scrolls and showing the optional live counter; the app does not
upload usage data or require an account.

## Detection note

Android exposes accessibility scroll events rather than a universal “reel
changed” event. The service therefore treats a debounced scroll event in a
supported app as one reel scroll. App updates can change their view hierarchy,
so the package allowlist and debounce are kept in one small service for easy
maintenance.
