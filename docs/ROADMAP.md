# Roadmap

| Phase | Work | Done when |
|---|---|---|
| **0. Foundations** | Project from the official template, `:protocol` module, private GitHub repo, CI | The starter app runs on the Pixel and CI passes |
| **1. Record the stock app** | Bluetooth HCI snoop log of the stock MK Tech app following a scripted set of moves; extract the broadcasts | A table of "control used → bytes broadcast" |
| **2. Protocol library** | Message builder and scrambler in `:protocol`, written test-first | Unit tests reproduce the recorded bytes exactly |
| **3. First motion** | Debug screen: Connect, one button per module, STOP | A motor on each module moves at the same time, driven by our code |
| **4. Drive screen** | Landscape controls, safety rules, connect guide | Drive and dig from one screen |
| **5. Settings** | Channel mapping, direction flip, speed caps, saved with DataStore | The mapping matches the machine and survives restarts |
| **6. Polish** | Game controller support, app icon, signed release build | An installable release APK |

## Safety rules (from Phase 3 on)

- Releasing a control returns that motor to neutral.
- If the app goes to the background or the screen locks, every motor goes neutral and the broadcast stops.
- A STOP button is always visible.
- A watchdog returns any motor to neutral if it hasn't been refreshed recently.
