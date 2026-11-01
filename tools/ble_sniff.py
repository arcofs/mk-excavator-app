"""Live BLE listener: print each change in manufacturer data for one company id.

Prints one line when a new payload is first heard (and how many times the
previous one was heard), so each line lines up with a control change on the
phone. Every line is also appended to a log file for later analysis.

Windows replays cached adverts about 60 s later with RSSI -127; those are
dropped so the log only shows what was actually on air.

Usage: python ble_sniff.py [--company FFF0] [--seconds 900] [--log path]
"""

import argparse
import asyncio
import sys
import time

from bleak import BleakScanner

CACHED_REPLAY_RSSI = -127


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--company", default="FFF0")
    ap.add_argument("--seconds", type=float, default=900)
    ap.add_argument("--log", default=None)
    args = ap.parse_args()
    company = int(args.company, 16)
    log = open(args.log, "a", encoding="utf-8") if args.log else None

    t0 = time.monotonic()
    last = None
    count = 0

    def out(line):
        print(line, flush=True)
        if log:
            log.write(line + "\n")
            log.flush()

    def on_advert(device, adv):
        nonlocal last, count
        data = adv.manufacturer_data.get(company)
        if data is None or adv.rssi <= CACHED_REPLAY_RSSI:
            return
        if data == last:
            count += 1
            return
        t = time.monotonic() - t0
        prev = f"(prev x{count})" if last is not None else "(first)"
        out(f"{t:8.2f}s {prev:>11} rssi={adv.rssi:4} {device.address} {data.hex(' ')}")
        last, count = data, 1

    out(f"listening for company 0x{company:04X} for {args.seconds:.0f}s")
    async with BleakScanner(detection_callback=on_advert, scanning_mode="active"):
        await asyncio.sleep(args.seconds)
    out(f"stopped; last payload heard x{count}")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        sys.exit(0)
