# MK Excavator

An Android app for driving the Mould King 13112 RC excavator. It controls both of the
excavator's 4.0 battery/Bluetooth modules at the same time, from one screen.

The stock app only drives one module at a time. Both modules listen to the same Bluetooth
broadcast, and each broadcast has room for three modules, so this app fills in two at once.

**Status:** Phases 0–3 done. The app's test bench drives both boxes at once on the real
machine. Protocol notes are in [docs/protocol.md](docs/protocol.md). Next is Phase 4: the real
drive screen. See [docs/ROADMAP.md](docs/ROADMAP.md).

**Using it:** switch on both boxes, open the app and tap **Connect**. Then short-press the
**track box's** power button once, so it gives two quick flashes (Module 2). Boxes forget this
when switched off, so it's needed on every power-up.

## Build and run

Needs Android Studio (or JDK 17+ and the Android SDK with API 37), and a phone on
Android 12 or newer with USB debugging turned on.

```bash
./gradlew assembleDebug                                 # build the debug APK
./gradlew installDebug                                  # install on the connected phone
./gradlew testDebugUnitTest :protocol:test lintDebug    # the checks CI runs
```

On Windows use `.\gradlew.bat` instead of `./gradlew`.

## Project layout

| Folder | What's in it |
|---|---|
| `app/` | The Android app: Compose UI, Bluetooth broadcaster, settings |
| `protocol/` | Pure Kotlin, no Android code: builds and scrambles the Mould King messages, tested on the PC |
| `docs/` | Roadmap and protocol notes |
| `tools/` | PC helpers. `ble_sniff.py` prints the Mould King broadcasts it hears, live (needs `pip install -r tools/requirements.txt`). `derive_checksum.py` shows how the checksum was found |

## Disclaimer

Independent hobby project. Not affiliated with or endorsed by Mould King.
