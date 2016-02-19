package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test

class PrettyPrintTest {
    @Test
    fun shouldDisplayInts() {
        val test2 = json { JsonObject(mapOf(
                "test" to mapOf<String, String>(),
                "2" to mapOf("test" to 22)
        )) }
        val string = test2.toJsonString(true)
        Assert.assertTrue(string.contains(" 22"), "22 should be displayed as an Int, not a String: $string")

    }
}
