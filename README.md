# ⚡ App Duper

**Run two accounts of the same app. One tap. Zero root.**

App Duper duplicates the apps on your phone so you can sign in to a second
account — two WhatsApps, two Instagrams, two of anything. Wrapped in a neon,
animation-heavy UI: a living starfield background, floating gradient orbs,
staggered card entrances, springy buttons, pulsing badges, and a confetti
burst every time a dupe lands.

## ✨ How it works

App Duper repackages the app's own APK under a new package id — the same
technique the "App Cloner" apps use. No root, no work profile:

1. Tap **Dupe** on any app.
2. App Duper pulls that app's installed APK, rewrites its package id in the
   binary `AndroidManifest.xml` (e.g. `com.instagram.android` →
   `com.instagram.android.dupe`), keeping every component class pointing at
   the original package so it still resolves, and making provider
   authorities and declared permissions unique so they don't collide with
   the original.
3. It re-signs the repackaged APK with its own bundled key
   ([`apksig`](https://android.googlesource.com/platform/tools/apksig/)).
4. It installs the result through `PackageInstaller` — you approve the
   normal Android install prompt, and a second, fully independent copy of
   the app appears in your launcher with its own data and its own login.

Manifest rewriting uses [ARSCLib](https://github.com/REAndroid/ARSCLib).
Open or remove any dupe straight from App Duper's home screen.

## 📱 Requirements

- Android 9 (API 28) or newer
- Permission to install unknown apps (Android prompts for this the first
  time you dupe something)

## ⚠️ Limits of this method

- Apps delivered as a split App Bundle are cloned including their config
  splits, but some heavily-obfuscated or integrity-checked apps (certain
  banking / DRM apps, apps with server-side signature pinning) may refuse
  to run when repackaged. Most social/messaging apps clone fine.
- A clone shares the original's name and icon in the launcher.

## 🔨 Building

```bash
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Or just push to GitHub — the **Build APK** workflow compiles it and uploads
the APK as a downloadable artifact on every push (Actions tab → latest run →
Artifacts).

## 🗂 Project layout

| Path | What it is |
|---|---|
| `core/ApkCloner.kt` | Pulls an app's APK(s) and rewrites the manifest package id via ARSCLib |
| `core/Signer.kt` | Re-signs the repackaged APK with the bundled key (apksig) |
| `core/CloneInstaller.kt` | Streams the signed clone into a `PackageInstaller` session |
| `core/Clones.kt` | Discovers/launches/removes installed clones (`.dupe` suffix) |
| `core/AppRepository.kt` | Loads every launchable app; decodes icons lazily |
| `install/InstallReceiver.kt` | Handles install-session status / the user-confirm prompt |
| `ui/` | Jetpack Compose UI: animated background, confetti, shimmer titles, the works |
