# Roadmap

| Phase | Work | Done when |
|---|---|---|
| **0. Foundations** ✅ | Project from the official template, `:protocol` module, private GitHub repo, CI | The starter app runs on the Pixel and CI passes |
| **1. Record the stock app** ✅ | Listen to the stock MK Tech app's broadcasts from the PC (`tools/ble_sniff.py`) while driving scripted moves, and map the channels on the machine | A table of "control used → bytes broadcast": [protocol.md](protocol.md) |
| **2. Protocol library** ✅ | `Mk4Message` and `MotorSpeeds` in `:protocol`, written test-first; checksum found with `tools/derive_checksum.py` | Unit tests reproduce the recorded bytes exactly |
| **3. First motion** ✅ | Test-bench screen: Connect, hold-to-run buttons per box, STOP; `BleAdvertiserRadio` and `MachineController` | A motor on each module moves at the same time, driven by our code |
| **4. Drive screen** ✅ | Landscape track levers and proportional joysticks (push forward lowers stick and boom), connect guide with an annotated photo of the boxes, swap boxes, safety rules | Drive and dig from one screen |
| **5. Settings** ✅ | Settings screen saved with DataStore: dead zone, axis snap, speed curve, top speed and Reverse per function. Also a latching STOP. (Channel mapping stayed fixed: the 13112's wiring is known, and Swap boxes covers the module mix-up.) | The controls feel right and survive restarts |
| **6. Polish** ✅ | New look: always-dark building-site theme, Barlow type, emergency-stop STOP, labelled stick bezels, excavator app icon. Signed release build (1.1, R8-shrunk, key outside the repo). Game controller: Xbox over Bluetooth, sticks as on screen, triggers drive the tracks forward and bumpers reverse them, B = STOP, ☰ = resume, touch locked while connected (tested on the machine) | An installable release APK |

## Safety rules (from Phase 3 on)

- Releasing a control returns that motor to neutral.
- If the app goes to the background or the screen locks, every motor goes neutral and the broadcast stops.
- A STOP button is always visible. It latches (yellow and black hazard stripes, "STOPPED · tap to
  resume", hazard tape along the screen edges), and a control held through the stop must be let
  go and pushed again before it works.
- A watchdog returns any motor to neutral if it hasn't been refreshed recently.
- Backstop in the hardware: a box stops its motors as soon as the broadcast stops (tested by
  force-closing the app mid-drive; see `docs/protocol.md`).
- A game controller follows the same rules: a held stick or trigger is re-sent every 100 ms,
  a controller that disconnects leaves every motor at rest, B latches STOP and only ☰ resumes, and
  anything pushed while driving is locked (guide open, stopped) must be let go before it works.
  The on-screen controls are locked while a controller is connected; the on-screen STOP is not.
