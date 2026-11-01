package io.github.arcofs.mkexcavator.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class Mk4MessageTest {
  private val captures: Map<String, ByteArray> =
    javaClass.getResourceAsStream("/mktech-2026-11-01.txt")!!
      .bufferedReader()
      .readLines()
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("#") }
      .associate { line ->
        val (label, bytes) = line.split("|").map { it.trim() }
        label to bytes.split(" ").map { it.toInt(16).toByte() }.toByteArray()
      }

  /** "idle" or space-separated "<module><channel><signed speed>", e.g. "1A+3 1B-3". */
  private fun speedsFor(label: String): MotorSpeeds =
    if (label == "idle") {
      MotorSpeeds.STOPPED
    } else {
      label.split(" ").fold(MotorSpeeds.STOPPED) { speeds, token ->
        speeds.with(token[0].digitToInt(), Channel.valueOf(token[1].toString()), token.substring(2).toInt())
      }
    }

  private fun ByteArray.hex() = joinToString(" ") { "%02x".format(it) }

  @Test
  fun reproducesEveryCapturedControlMessage() {
    val controls = captures.filterKeys { it != "connect" }
    assertEquals(22, controls.size)
    for ((label, expected) in controls) {
      assertEquals(label, expected.hex(), Mk4Message.control(speedsFor(label)).hex())
    }
  }

  @Test
  fun reproducesTheCapturedConnectMessage() {
    assertEquals(captures.getValue("connect").hex(), Mk4Message.connect().hex())
  }

  @Test
  fun messagesFitTheManufacturerDataSize() {
    assertEquals(Mk4Message.SIZE, Mk4Message.connect().size)
    assertEquals(Mk4Message.SIZE, Mk4Message.control(MotorSpeeds.STOPPED).size)
  }

  @Test
  fun eachModuleOnlyChangesItsOwnBytes() {
    val stopped = Mk4Message.control(MotorSpeeds.STOPPED)
    for (module in 1..MotorSpeeds.MODULES) {
      for (channel in Channel.entries) {
        val message = Mk4Message.control(MotorSpeeds.STOPPED.with(module, channel, 5))
        val changed = message.indices.filter { message[it] != stopped[it] }.toSet()
        val ownByte = 11 + (module - 1) * 2 + channel.ordinal / 2
        assertEquals("module $module channel $channel", setOf(ownByte, 18, 19), changed)
      }
    }
  }

  @Test
  fun drivesTwoModulesInOneMessage() {
    val arm = MotorSpeeds.STOPPED.with(1, Channel.A, 6)
    val tracks = MotorSpeeds.STOPPED.with(2, Channel.A, 6)
    val both = Mk4Message.control(arm.with(2, Channel.A, 6))

    assertEquals(captures.getValue("1A+6")[11], both[11])
    assertEquals(captures.getValue("2A+6")[13], both[13])
    assertNotEquals(Mk4Message.control(arm).hex(), both.hex())
    assertNotEquals(Mk4Message.control(tracks).hex(), both.hex())
  }

  @Test
  fun returnsAFreshArrayEachTime() {
    val first = Mk4Message.control(MotorSpeeds.STOPPED)
    first.fill(0)
    assertEquals(captures.getValue("idle").hex(), Mk4Message.control(MotorSpeeds.STOPPED).hex())

    val connect = Mk4Message.connect()
    connect.fill(0)
    assertEquals(captures.getValue("connect").hex(), Mk4Message.connect().hex())
  }
}
