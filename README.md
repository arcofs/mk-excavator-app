# MK Excavator

An Android app for driving the Mould King 13112 RC excavator. It controls both of the
excavator's 4.0 battery/Bluetooth modules at the same time, from one screen.

The stock app only drives one module at a time. Both modules listen to the same Bluetooth
broadcast, and each broadcast has room for three modules, so this app fills in two at once.

**Status:** Phases 0–5 done and Phase 6 under way: the excavator drives and digs from one screen,
with adjustable controls, a dark building-site look and its own icon. Protocol notes are
in [docs/protocol.md](docs/protocol.md). See [docs/ROADMAP.md](docs/ROADMAP.md) for what's next.

## Using it

1. Switch on both battery boxes, open the app and tap **Connect**.
2. The guide shows which box is which. Short-press the **track box's** power button once, so it
   flashes twice (Module 2), then tap **Done**. Boxes forget this when switched off, so it's
   needed on every power-up. If you press the arm box by mistake, tap "The arm box flashes twice
   instead".
3. Drive. Each track has a lever at the screen edge. The left joystick slews and moves the stick;
   the right joystick tilts the bucket and moves the boom. As on a real excavator, pushing a
   joystick forward lowers the stick or boom. Speed follows how far you push. The rim of each
   control says what each direction does, and that label lights up while it's driving.

Letting go stops that motor and leaving the app disconnects. **STOP** stops everything and
latches (hazard stripes, and hazard tape along the screen edges): tap it again to resume. The
**Arm 1 · Track 2** button under it swaps the boxes if the button press went to the wrong one.

**Game controller.** Pair an Xbox (or other Bluetooth) controller in Android's Bluetooth settings
and it drives from the drive screen: the sticks work like the on-screen joysticks, each trigger
runs its track forward and holding that side's bumper makes it reverse, **B** is STOP and **☰**
resumes. While it's connected the on-screen sticks follow it and ignore touch; the on-screen STOP
still works. If it disconnects, everything stops. The full button map is in Settings.

**Settings** (the gear on the drive screen) adjusts the dead zone, how much sideways drift a
joystick ignores, the speed curve, and each function's top speed and direction. Changes are saved
on the phone and apply straight away. The test bench is linked from the settings page.

## Build and run

Needs Android Studio (or JDK 17+ and the Android SDK with API 37), and a phone on
Android 12 or newer with USB debugging turned on.

```bash
./gradlew assembleDebug                                 # build the debug APK
./gradlew installDebug                                  # install on the connected phone
./gradlew testDebugUnitTest :protocol:test lintDebug    # the checks CI runs
```

On Windows use `.\gradlew.bat` instead of `./gradlew`.

Development builds install as **MK Excavator Dev** (`io.github.arcofs.mkexcavator.debug`), so they
sit next to the released app instead of replacing it.

### Release build

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

This makes a shrunk APK, signed so it installs on any phone with Android 12 or newer, without
USB debugging: copy the file over and open it, and Android asks once to allow installs from that
source. The signing key is kept outside the repo, in `~/keystores/mk-excavator/` (or wherever the
Gradle property `mkexcavator.signing` points), as `release.jks` plus a `keystore.properties`:

```properties
storeFile=C:/Users/you/keystores/mk-excavator/release.jks
storePassword=...
keyAlias=mk-excavator
keyPassword=...
```

Without that file the release build still works but comes out unsigned, as on CI. **Back up both
files.** Every update must be signed with the same key, or Android won't install it over the app.
For each new release, raise `versionCode` (and `versionName`) in `app/build.gradle.kts`.

## Project layout

| Folder | What's in it |
|---|---|
| `app/` | The Android app: Compose UI, Bluetooth broadcaster, settings |
| `protocol/` | Pure Kotlin, no Android code: builds and scrambles the Mould King messages, tested on the PC |
| `docs/` | Roadmap and protocol notes |
| `tools/` | PC helpers. `ble_sniff.py` prints the Mould King broadcasts it hears, live (needs `pip install -r tools/requirements.txt`). `derive_checksum.py` shows how the checksum was found |

## Credits

- [Barlow](https://github.com/jpt/barlow) typeface, SIL Open Font License 1.1.
- Icons from [Material Symbols](https://github.com/google/material-design-icons), Apache License 2.0.
- The excavator icon was drawn for this app.

Both licence texts ship in the app, in `app/src/main/assets/licenses/`.

## Disclaimer

Independent hobby project. Not affiliated with or endorsed by Mould King.
