package com.beust.klaxon

import kotlin.test.assertEquals
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import kotlin.test.assertNotNull

class KlaxonTest {
    BeforeClass
    fun bc() {
    }

    private fun read(name: String) : JsonObject  {
        val cls = javaClass<KlaxonTest>()
        return Parser().parse(cls.getResourceAsStream(name)!!)
    }

    Test
    fun simple() {
        val j = read("/b.json")!!
        val expected = JsonArray()
                .add(JsonLong(1))
                .add(JsonString("abc"))
                .add(JsonDouble(2.34))
                .add(JsonBoolean(false))
        assertEquals(expected, j)
    }

    Test
    fun basic() {
        val j = read("/a.json")!!
        val expected = JsonObject()
            .put(JsonString("a"), JsonString("b"))
            .put(JsonString("c"), JsonArray()
                .add(JsonLong(1))
                .add(JsonDouble(2.34))
                .add(JsonString("abc"))
                .add(JsonBoolean(false)))

        assertEquals(expected, j)
    }
}

