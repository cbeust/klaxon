package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertEquals

@Test
class TestDate {

    fun date() {

        val cls = KlaxonTest::class.java
        val dateJson = Parser().parse(cls.getResourceAsStream("date.json")!!) as JsonObject
        val expected = Iso8601Utils.parse("1994-11-05T13:15:30Z")
        assertEquals(expected, dateJson.date("date", { Iso8601Utils.parse(it) }))

    }

}