package io.github.arcofs.mkexcavator.protocol

/** A motor port on a Mould King 4.0 battery box. */
enum class Channel { A, B, C, D }

/**
 * The speed of every channel on each of the three module numbers one broadcast can address.
 *
 * A battery box is module 1 after power-on; each short press of its power button while connected
 * moves it to the next module. Speeds run from -[MAX_SPEED] to [MAX_SPEED], and 0 stops the motor.
 * Positive is the direction MK Tech sends for its stick pushed up (channels A and C) or left
 * (channels B and D).
 *
 * Immutable: [with] returns a copy.
 */
class MotorSpeeds private constructor(private val speeds: IntArray) {
  operator fun get(module: Int, channel: Channel): Int = speeds[index(module, channel)]

  fun with(module: Int, channel: Channel, speed: Int): MotorSpeeds {
    require(speed in -MAX_SPEED..MAX_SPEED) { "speed must be in -$MAX_SPEED..$MAX_SPEED (was $speed)" }
    val index = index(module, channel)
    if (speeds[index] == speed) return this
    return MotorSpeeds(speeds.copyOf().also { it[index] = speed })
  }

  override fun equals(other: Any?): Boolean = other is MotorSpeeds && speeds.contentEquals(other.speeds)

  override fun hashCode(): Int = speeds.contentHashCode()

  override fun toString(): String =
    (1..MODULES).joinToString(prefix = "MotorSpeeds(", postfix = ")") { module ->
      "$module:" + Channel.entries.joinToString("") { channel -> "${channel.name}${this[module, channel]}" }
    }

  companion object {
    const val MODULES = 3
    const val MAX_SPEED = 7

    val STOPPED = MotorSpeeds(IntArray(MODULES * Channel.entries.size))

    private fun index(module: Int, channel: Channel): Int {
      require(module in 1..MODULES) { "module must be in 1..$MODULES (was $module)" }
      return (module - 1) * Channel.entries.size + channel.ordinal
    }
  }
}
