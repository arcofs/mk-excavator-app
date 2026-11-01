package io.github.arcofs.mkexcavator.protocol

/**
 * Builds the BLE manufacturer data that Mould King 4.0 battery boxes obey.
 *
 * Worked out from MK Tech's own broadcasts; the layout and how it was derived are in
 * docs/protocol.md. Every control message is MK Tech's idle message with two changes:
 * - each channel's nibble in bytes 11-16 (two bytes per module: A|B, then C|D), and
 * - the checksum in bytes 18-19: CRC-16/KERMIT of bytes 8-17, little-endian, XOR [CHECKSUM_OFFSET].
 */
object Mk4Message {
  /** Manufacturer ID to advertise the message under. */
  const val COMPANY_ID = 0xFFF0

  /** Every message is this many bytes. */
  const val SIZE = 24

  // MK Tech's control message with every channel stopped.
  private val IDLE = hex("6d b6 43 cf 7e 8f 47 11 98 bc cb 30 d9 72 a2 3c 6f 56 f4 84 15 16 17 18")

  // What MK Tech sends after connecting, until the first control moves.
  private val CONNECT = hex("6d b6 43 cf 7e 8f 47 11 48 bc cb 38 d1 7a 29 e6 33 c2 13 14 15 16 17 18")

  private const val FIRST_CHANNEL_BYTE = 11
  private const val CHECKSUMMED_FROM = 8
  private const val CHECKSUM_BYTE = 18

  // Absorbs what the checksum covers that we can't see (the start of the packet) and the
  // scrambling applied to the checksum bytes. Derived from IDLE.
  private val CHECKSUM_OFFSET = intArrayOf(0xBA, 0xC4)

  // A channel's nibble: bit 3 = direction (set for negative speeds), bits 0-2 = speed.
  private const val STOPPED_NIBBLE = 0x8

  fun connect(): ByteArray = CONNECT.copyOf()

  fun control(speeds: MotorSpeeds): ByteArray {
    val message = IDLE.copyOf()
    for (module in 1..MotorSpeeds.MODULES) {
      for (channel in Channel.entries) {
        // The idle message holds every channel's stopped nibble, already scrambled. Scrambling is
        // a fixed XOR, so flipping the bits that differ from "stopped" gives the scrambled value.
        val change = nibble(speeds[module, channel]) xor STOPPED_NIBBLE
        if (change == 0) continue
        val slot = (module - 1) * Channel.entries.size + channel.ordinal
        val byte = FIRST_CHANNEL_BYTE + slot / 2
        val shift = if (slot % 2 == 0) 4 else 0
        message[byte] = (message[byte].toInt() xor (change shl shift)).toByte()
      }
    }
    val crc = crc16Kermit(message, CHECKSUMMED_FROM, CHECKSUM_BYTE)
    message[CHECKSUM_BYTE] = ((crc and 0xFF) xor CHECKSUM_OFFSET[0]).toByte()
    message[CHECKSUM_BYTE + 1] = ((crc ushr 8) xor CHECKSUM_OFFSET[1]).toByte()
    return message
  }

  private fun nibble(speed: Int): Int = if (speed > 0) speed else STOPPED_NIBBLE or -speed

  private fun hex(bytes: String): ByteArray = bytes.split(" ").map { it.toInt(16).toByte() }.toByteArray()
}

/** CRC-16/KERMIT (poly 0x1021 reflected, init 0, no final XOR) of `data[from until to]`. */
internal fun crc16Kermit(data: ByteArray, from: Int, to: Int): Int {
  var crc = 0
  for (i in from until to) {
    crc = crc xor (data[i].toInt() and 0xFF)
    repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408 else crc ushr 1 }
  }
  return crc
}
