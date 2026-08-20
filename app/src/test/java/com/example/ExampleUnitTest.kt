package com.example

import com.example.sync.LocalNetworkSyncServer
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun syncPin_generationIsValid() {
    for (i in 1..20) {
      val pin = LocalNetworkSyncServer.generateRandomPin()
      assertEquals(6, pin.length)
      assertTrue(pin.all { it.isDigit() })
      val num = pin.toInt()
      assertTrue(num in 100000..999999)
    }
  }
}
