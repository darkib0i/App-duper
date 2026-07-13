# ⚡ App Duper

**Run two accounts of the same app. One tap. Zero root.**

App Duper duplicates the apps on your phone so you can sign in to a second
account — two WhatsApps, two Instagrams, two of anything. Wrapped in a neon,
animation-heavy UI: a living starfield background, floating gradient orbs,
staggered card entrances, springy buttons, pulsing badges, and a confetti
burst every time a dupe lands.

## ✨ How it works

App Duper is built on Android's **managed work profile** — the same
battle-tested isolation the OS uses for work apps (and that open-source
cloners like Shelter and Island use):

1. On first launch you tap **Create Dupe Space** — Android creates an
   isolated profile owned by App Duper.
2. Tap **Dupe** on any app — App Duper sends a command across the profile
   boundary and installs a second copy of that app inside the Dupe Space
   (no APK downloading, it reuses the app already on your phone).
3. The dupe shows up in your launcher's work tab with a briefcase badge and
   runs with **completely separate data** — so it gets its own login.

You can open or remove any dupe straight from App Duper's home screen.

## 📱 Requirements

- Android 9 (API 28) or newer
- A device that allows creating a work profile (phones managed by a
  school/employer may block this)

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
| `core/Profiles.kt` | Finds/creates the Dupe Space, sends cross-profile commands, lists & launches dupes |
| `core/AppRepository.kt` | Loads every launchable app with its icon |
| `admin/DuperAdmin.kt` | Profile-owner receiver; finalizes the Dupe Space after provisioning |
| `admin/ProvisioningActivity.kt` | Android 12+ provisioning hooks |
| `bridge/ProfileBridgeActivity.kt` | Invisible worker inside the Dupe Space that installs/uninstalls dupes |
| `ui/` | Jetpack Compose UI: animated background, confetti, shimmer titles, the works |

## ⚠️ Notes

- A few apps detect work profiles and refuse to run cloned (some banking
  apps, DRM-heavy apps). Most social/messaging apps dupe fine.
- Removing the Dupe Space (Settings → Accounts → Remove work profile)
  deletes all dupes and their data at once.
