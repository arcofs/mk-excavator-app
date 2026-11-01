package io.github.arcofs.mkexcavator.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class Crc16KermitTest {
  @Test
  fun matchesTheStandardCheckValue() {
    // Every CRC catalogue lists CRC-16/KERMIT("123456789") = 0x2189.
    val data = "123456789".toByteArray(Charsets.US_ASCII)
    assertEquals(0x2189, crc16Kermit(data, 0, data.size))
  }

  @Test
  fun coversOnlyTheGivenRange() {
    val data = "xx123456789yy".toByteArray(Charsets.US_ASCII)
    assertEquals(0x2189, crc16Kermit(data, 2, 11))
  }
}
