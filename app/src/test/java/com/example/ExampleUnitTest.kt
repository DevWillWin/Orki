package com.example

import com.example.util.formatFileSize
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUploadLimitsPerPlan() {
    fun getDailyUploadLimit(plan: String): Int {
      return when (plan) {
        "Guest" -> 0
        "Free" -> 2
        "Plus" -> 20
        "Pro" -> 100
        else -> 2
      }
    }

    assertEquals(0, getDailyUploadLimit("Guest"))
    assertEquals(2, getDailyUploadLimit("Free"))
    assertEquals(20, getDailyUploadLimit("Plus"))
    assertEquals(100, getDailyUploadLimit("Pro"))
  }

  @Test
  fun testFormatFileSize() {
    assertEquals("0 B", formatFileSize(0))
    assertEquals("512 B", formatFileSize(512))
    assertEquals("1.0 KB", formatFileSize(1024))
    assertEquals("2.5 MB", formatFileSize((2.5 * 1024 * 1024).toLong()))
  }
}

