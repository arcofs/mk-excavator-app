# Mould King 4.0 module protocol (as observed)

Everything here comes from our own captures of the stock **MK Tech 2.6.5** app on a Pixel 8 Pro,
heard live from a Windows PC with `tools/ble_sniff.py` on 2026-11-01, with the 13112's two 4.0
battery boxes powered on and watched while each control was used. The captured messages are in
[`protocol/src/test/resources/mktech-2026-11-01.txt`](../protocol/src/test/resources/mktech-2026-11-01.txt).

Status:
- The layout and channel map are **confirmed on the machine**.
- The checksum is **identified**. `Mk4Message` in `:protocol` reproduces every captured message
  byte for byte, and its tests check that.
- Still to confirm on the machine (Phase 3): channels we never captured, such as Module 2's C,
  and the speed each value actually gives.

## Transport

- The phone sends **BLE advertisements** only. There is no pairing and no connection.
- The advertisement carries **manufacturer-specific data under company ID `0xFFF0`, 24 bytes long**.
  Android reports it as legacy, connectable advertising at a 100 ms interval.
- MK Tech restarts its advertisement on every change, so the phone's Bluetooth address changes
  with every new message. The boxes don't care.
- The boxes only listen. Nothing was heard from them.

## Message layout

Bytes numbered 0–23.

| Bytes | Control message | Notes |
|---|---|---|
| 0–7 | `6d b6 43 cf 7e 8f 47 11` | Never changes, including in the connect message |
| 8–10 | `98 bc cb` | Header. The connect message has `48 bc cb` |
| **11–12** | **Module 1** | Byte 11: channel A (high nibble), B (low nibble). Byte 12: C (high), D (low) |
| **13–14** | **Module 2** | Same layout |
| **15–16** | **Module 3** | Same layout |
| 17 | `56` | Never changes in control messages |
| 18–19 | checksum | Changes whenever any earlier content changes |
| 20–23 | `15 16 17 18` | Padding: the byte at index *i* is *i* + 1 |

The **connect message** is two bytes shorter: content in bytes 8–15, checksum in 16–17, then the
same padding rule (`13 14 15 16 17 18`).

```
connect  6d b6 43 cf 7e 8f 47 11 48 bc cb 38 d1 7a 29 e6 33 c2 13 14 15 16 17 18
idle     6d b6 43 cf 7e 8f 47 11 98 bc cb 30 d9 72 a2 3c 6f 56 f4 84 15 16 17 18
```

**Every control message carries all three modules.** MK Tech only fills in the module selected
at the top of its screen and leaves the others at rest, but nothing in the format stops us from
driving Module 1 and Module 2 in the same message. That's how this app drives the tracks and
the arm at the same time.

## Channel values

Moving one stick changes only that channel's nibble plus the checksum. So the scrambling is a
fixed XOR applied position by position, and it doesn't spread changes across bytes.

Reading each scrambled nibble as XORed with a fixed per-nibble key, with a channel at rest as
`0x8`, slowly pushing a stick decodes to steadily rising values:

| MK Tech stick | Decoded nibble | Meaning |
|---|---|---|
| at rest | `8` | speed 0 |
| up / left, pushed further and further | `1` … `7` | direction bit 0, speed 1–7 |
| down / right, pushed further and further | `9` … `F` | direction bit 1, speed 1–7 |

So each channel is 4 bits: **bit 3 = direction, bits 0–2 = speed 0–7**. Speed 7 was seen once
(channel D pushed fully left). The scripted pushes stopped about 86% of the way out and gave 6.

The per-nibble keys implied by that reading, from the idle message:

| Module | A | B | C | D |
|---|---|---|---|---|
| 1 | `B` | `8` | `5` | `1` |
| 2 | `F` | `A` | `2` | `A` |
| 3 | `B` | `4` | `E` | `7` |

We don't need to know the box's own idea of "at rest" (`0x8`, or `0x0` with keys that differ by 8).
`Mk4Message` starts from MK Tech's idle message and flips only the bits that differ from a stopped
channel, so it sends exactly what MK Tech sends for the same stick position either way.

The speed numbers are our reading of the nibble, ordered by how far the stick was pushed. Phase 3
checks that higher numbers really do drive the motors faster.

## Checksum

Bytes 18–19 are **CRC-16/KERMIT** (polynomial `0x1021` reflected, init 0, no final XOR) of bytes
8–17, little-endian, then XORed with `ba c4`:

```
byte 18 = (crc & 0xff) ^ 0xba
byte 19 = (crc >> 8)   ^ 0xc4
```

How it was found: scrambling is a fixed XOR, so a CRC's change depends only on the content's
change. `tools/derive_checksum.py` tried every 16-bit polynomial with both bit orders and both
byte orders against the 21 control captures. **Exactly one combination predicts every checksum
change.** The constant `ba c4` comes from the idle message. It absorbs what the CRC also covers
but we never see (the start of the packet) and the scrambling of the checksum bytes. With it, the
formula reproduces all 22 captured control messages.

The connect message has no adjustable content, so it's sent exactly as captured.

## Module numbers

- Every box is **Module 1** after power-on.
- While MK Tech is connected, a **short press of a box's power button** moves it to the next
  module: two quick flashes for Module 2, three for Module 3. This resets to Module 1 when the
  box is switched off.
- A box obeys only its own module's two bytes.

## Connecting

- Turning on MK Tech's Bluetooth icon starts the **connect message**, repeated continuously.
- MK Tech keeps sending it until the first control moves, then switches to control messages. At
  rest that's the idle message.
- The manual says a box must be powered on before the app connects, so a box presumably only
  accepts the connect message during a short window after power-on. Still to measure.

## 13112 channel map

Confirmed on the machine with the arm box on Module 1 and the track box on Module 2. Directions
are MK Tech's stick directions.

| Box (module) | Channel | Function | Stick up / left | Stick down / right |
|---|---|---|---|---|
| Arm box (1) | A | Bucket tilt | open / up | close |
| Arm box (1) | B | Stick (end of arm) | down | up |
| Arm box (1) | C | Slew | left | right |
| Arm box (1) | D | Boom (whole arm) | up | down |
| Track box (2) | A | Right track | forward | reverse |
| Track box (2) | C | Left track | forward | reverse |
| Track box (2) | B, D | not used | | |

Observed directly: bucket open on A up, stick up on B right, slew left on C up, boom down on D
right, right track forward on A up, left track forward on C up. The opposite directions are
inferred.

With both boxes left on Module 1, one channel drives both boxes. A moves the bucket **and** the
right track, and C moves the slew **and** the left track. That is the "mixed up channels" effect
seen in MK Tech before the track box was moved to Module 2.

## Open questions

- Channels we never captured (Module 2's B, C and D, and Module 3's B, C and D). They follow the
  same layout and checksum, so they should work, but they still need confirming on the machine.
- How long the connect message must be sent, and whether a box can join later.
- What a box does when messages stop: does it hold its last command or stop? This matters for
  the safety rules.
