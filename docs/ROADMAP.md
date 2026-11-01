# Roadmap

| Phase | Work | Done when |
|---|---|---|
| **0. Foundations** ✅ | Project from the official template, `:protocol` module, private GitHub repo, CI | The starter app runs on the Pixel and CI passes |
| **1. Record the stock app** ✅ | Listen to the stock MK Tech app's broadcasts from the PC (`tools/ble_sniff.py`) while driving scripted moves, and map the channels on the machine | A table of "control used → bytes broadcast": [protocol.md](protocol.md) |
| **2. Protocol library** ✅ | `Mk4Message` and `MotorSpeeds` in `:protocol`, written test-first; checksum found with `tools/derive_checksum.py` | Unit tests reproduce the recorded bytes exactly |
| **3. First motion** ✅ | Test-bench screen: Connect, hold-to-run buttons per box, STOP; `BleAdvertiserRadio` and `MachineController` | A motor on each module moves at the same time, driven by our code |
| **4. Drive screen** ✅ | Landscape track levers and proportional joysticks (push forward lowers stick and boom), connect guide with an annotated photo of the boxes, swap boxes, safety rules | Drive and dig from one screen |
| **5. Settings** | Channel mapping, direction flip, speed caps, saved with DataStore | The mapping matches the machine and survives restarts |
| **6. Polish** | Game controller support, app icon, signed release build | An installable release APK |

## Safety rules (from Phase 3 on)

- Releasing a control returns that motor to neutral.
- If the app goes to the background or the screen locks, every motor goes neutral and the broadcast stops.
- A STOP button is always visible.
- A watchdog returns any motor to neutral if it hasn't been refreshed recently.
- Backstop in the hardware: a box stops its motors as soon as the broadcast stops (tested by
  force-closing the app mid-drive; see `docs/protocol.md`).
