# Auto Loop Karoo

A [Hammerhead Karoo 3](https://www.hammerhead.io/karoo) extension that automatically scrolls through your ride data pages — so you can focus on riding.

---

## Features

- **Auto-scroll** cycles through all pages in your active ride profile at a configurable interval
- **Starts automatically** when you begin recording a ride
- **Stops automatically** when you pause or end the ride
- **Navigation-aware**: switches to the map page when approaching a turn (<50 m), then resumes scrolling 20 m after the turn
- **Toggle on/off** from the app settings at any time
- **Audible feedback** (beep + on-screen alert) when auto-scroll is enabled or disabled

---

## Requirements

- Hammerhead Karoo 3
- Karoo firmware with extension support

---

## Installation

1. Download the latest APK from [Releases](https://github.com/anpaiss/AutoLoopKaroo/releases)
2. Install via ADB:
   ```bash
   adb install AutoLoopKaroo.apk
   ```
   Or transfer the APK to the Karoo via USB and install with a file manager

No additional profile configuration is required — the extension starts automatically with the Karoo AppStore.

---

## Configuration

Open the **Auto Loop Karoo** app on your Karoo:

| Setting | Description |
|---|---|
| **Auto Scroll** toggle | Enable or disable auto-scrolling |
| **Time per page** | Seconds to display each page before advancing (1–30 s, default 5 s) |

Changes take effect immediately — no restart needed.

---

## How It Works

```
Ride starts (Recording)
    └─ Auto-scroll enabled? ──Yes──► Start cycling pages every N seconds
                             │
                             No ──► Wait (toggle from app to enable)

Approaching turn (<50 m)
    └─► Switch to map page automatically

20 m past the turn
    └─► Resume page scrolling

Ride paused or ended
    └─► Stop scrolling
```

---

## Building from Source

```bash
git clone https://github.com/anpaiss/AutoLoopKaroo.git
cd AutoLoopKaroo
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:** Android Studio / JDK 17+, Android SDK 34.

---

## License

MIT License — see [LICENSE](LICENSE) for details.
