![Total Downloads](https://img.shields.io/github/downloads/anpaiss/AutoLoopKaroo/total?color=blue)
![Latest Release](https://img.shields.io/github/v/release/anpaiss/AutoLoopKaroo)
# Auto Loop Karoo

A [Hammerhead Karoo 3](https://www.hammerhead.io/karoo) extension that automatically scrolls through your ride data pages — so you can focus on riding.

---

## Features

- **Auto-scroll** cycles through all pages in your active ride profile at a configurable interval.
- **Page Skipping**: Set a page duration to **0 seconds** to skip it during the loop.
- **Starts automatically** when you begin recording a ride.
- **Stops automatically** when you pause or end the ride.
- **Navigation-aware**: Switches to the map page when approaching a turn, then resumes scrolling after the turn. Distances are fully configurable.
- **Toggle on/off** from the app settings or via **Control Center Bonus Actions**.
- **Audible feedback** (beep + on-screen alert) when auto-scroll is toggled.

---

## Requirements

- Hammerhead Karoo 3 (Hardware ID: k24)
- Karoo firmware with extension support

---

## Installation

1. Download the latest APK (Release Candidate 0.9.5+) from [Releases](https://github.com/anpaiss/AutoLoopKaroo/releases)
2. Install via ADB:
   ```bash
   adb install AutoLoopKaroo.apk
   ```
   Or transfer the APK to the Karoo via USB and install with a file manager.

*Note: After installation, it is recommended to reboot the Karoo to ensure the extension service is fully registered.*

---

## Configuration

Open the **Auto Loop Karoo** app on your Karoo:

| Setting | Description |
|---|---|
| **Auto Scroll** toggle | Enable or disable auto-scrolling globally. |
| **Time per page** | Seconds to display each page. Set to **Skip** (0s) to ignore a page in the loop. |
| **Sound Feedback** | Toggle audible beeps when enabling/disabling scroll. |
| **Switch-to-map distance** | Distance before a turn to switch to the map page (10–250 m). |
| **Resume distance** | Distance after a turn before auto-scroll resumes (10–250 m). |

Changes take effect immediately — no restart needed.

---

## How It Works

```
Ride starts (Recording)
    └─ Auto-scroll enabled? ──Yes──► Start cycling pages based on per-page dwell time
                             │       (Skips pages set to 0s)
                             No ──► Wait (toggle from app or Control Center)

Approaching turn (within switch-to-map distance, default 25 m)
    └─► Switch to map page automatically

Past the turn by resume distance (default 10 m)
    └─► Resume page scrolling

Ride paused or ended
    └─► Stop scrolling
```

---

## Development Notes

- **Target SDK 31**: Aligned with Karoo 3 (Android 12) for maximum stability and input handling.
- **Extension ID**: `autoloopkaroo` (No dots, as required by Hammerhead SDK).
- **Bonus Actions**: Triggerable via SRAM AXS buttons, external ANT+ controllers, or the Control Center widget.

---

## License

MIT License — see [LICENSE](LICENSE) for details.

## Screenshots

<table>
  <tr>
    <td><img width="240" height="400" alt="autoloopCONFIG1" src="https://github.com/user-attachments/assets/636b0540-a781-468c-be6f-ed9d666e9a88" /></td>
    <td> <img width="240" height="400" alt="beta095-2" src="https://github.com/user-attachments/assets/0b8156fd-43a6-4727-aff6-39d4483eae01" /></td>
  </tr>
</table>

<table>
  <tr>
    <td><img width="240" height="400" alt="autoloopON" src="https://github.com/user-attachments/assets/8354d8c4-717c-4083-9853-59ba82d8bea7" /></td>
    <td>  <img width="240" height="400" alt="autoloopOFF" src="https://github.com/user-attachments/assets/70fad57b-43f0-4aee-9340-37add7d03cbb" /></td>
  </tr>
</table>



