# :protocol

Pure Kotlin library (no Android dependencies) that turns motor commands into the bytes the
Mould King 4.0 modules expect: the connect message, the motor message for up to three
modules, and the scrambling applied before broadcast.

- `MotorSpeeds`: speed -7..7 for each channel A-D on modules 1-3. Immutable.
- `Mk4Message.control(speeds)`: the 24-byte manufacturer data for those speeds, all modules in one message.
- `Mk4Message.connect()`: the message to send before the first control message.
- Advertise either under manufacturer ID `Mk4Message.COMPANY_ID` (`0xFFF0`).

The unit tests check the output against broadcasts recorded from the stock app
(`src/test/resources/mktech-2026-11-01.txt`). Protocol details: `docs/protocol.md`.
