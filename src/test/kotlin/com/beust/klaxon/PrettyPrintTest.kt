package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

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

    @Test
    fun shouldDisplayDate() {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        val expectedDate = Date.from(Instant.now())
        val expectedString = simpleDateFormat.format(expectedDate)
        val json = json { JsonObject(mapOf(
                "test" to mapOf<String, String>(),
                "date" to expectedDate
        )) }
        val modifier = Modifier({ simpleDateFormat.format(it) })
        val string = json.toJsonString(true, modifier = modifier)
        Assert.assertTrue(
                string.contains(expectedString),
                "Date needs to be formatted, resulting in a String like: $expectedString")
    }

}
