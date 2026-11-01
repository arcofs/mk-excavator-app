"""Find the checksum of the Mould King control message from captured broadcasts.

Every capture differs from the idle message only in its channel bytes (8-17) and its
two checksum bytes (18-19). Scrambling is a fixed XOR per byte, so for any CRC the
checksum change depends only on the content change:

    crc(idle ^ delta) ^ crc(idle) == crc0(delta)

where crc0 is the bare polynomial remainder (no init, no final XOR). So we try every
16-bit polynomial, both bit orders in and out and both byte orders, and keep those
that predict the checksum change of every capture. No knowledge of the keystream,
the address bytes or the CRC's init value is needed.

Usage: python -I derive_checksum.py <captures.txt>
"""

import sys

CONTENT = range(8, 18)  # bytes covered by the checksum that we can see change
CHECKSUM = (18, 19)


def load(path):
    vectors = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            label, hexbytes = (part.strip() for part in line.split("|"))
            vectors[label] = bytes.fromhex(hexbytes)
    return vectors


def rev8(b):
    return int(f"{b:08b}"[::-1], 2)


def bit_distances(delta, reverse_input):
    """Distance from the end of the checksummed region for each set bit, MSB-first."""
    n = len(delta)
    out = []
    for j, byte in enumerate(delta):
        if reverse_input:
            byte = rev8(byte)
        for b in range(8):
            if byte >> b & 1:
                out.append(8 * (n - 1 - j) + b)
    return out


def responses(poly, count):
    """x^(16+k) mod P for k = 0..count-1, as 16-bit values."""
    r = poly  # x^16 mod P
    out = []
    for _ in range(count):
        out.append(r)
        r <<= 1
        if r & 0x10000:
            r = (r ^ poly) & 0xFFFF
    return out


OUTPUTS = {
    "big-endian": lambda c: (c >> 8, c & 0xFF),
    "little-endian": lambda c: (c & 0xFF, c >> 8),
    "big-endian, bits reversed": lambda c: (rev8(c >> 8), rev8(c & 0xFF)),
    "little-endian, bits reversed": lambda c: (rev8(c & 0xFF), rev8(c >> 8)),
}


def main():
    vectors = load(sys.argv[1])
    idle = vectors["idle"]
    cases = []
    for label, v in vectors.items():
        if label in ("idle", "connect"):
            continue
        delta = bytes(v[i] ^ idle[i] for i in CONTENT)
        want = tuple(v[i] ^ idle[i] for i in CHECKSUM)
        cases.append((label, delta, want))
    print(f"{len(cases)} control captures, content bytes {CONTENT.start}-{CONTENT.stop - 1}")

    found = []
    for reverse_input in (False, True):
        dists = [(label, bit_distances(d, reverse_input), want) for label, d, want in cases]
        for poly in range(1, 0x10000, 2):
            resp = responses(poly, 8 * len(CONTENT))
            for name, split in OUTPUTS.items():
                for label, ds, want in dists:
                    c = 0
                    for k in ds:
                        c ^= resp[k]
                    if split(c) != want:
                        break
                else:
                    found.append((poly, reverse_input, name))

    if not found:
        print("No 16-bit CRC explains every capture.")
        return 1
    for poly, reverse_input, name in found:
        print(f"MATCH poly=0x{poly:04X}  input bits {'reversed' if reverse_input else 'as sent'}  output {name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
