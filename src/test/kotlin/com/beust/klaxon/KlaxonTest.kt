package com.beust.klaxon

import kotlin.test.assertEquals
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import kotlin.test.assertNotNull

class KlaxonTest {
    BeforeClass
    fun bc() {
    }

    private fun read(name: String) : Any?  {
        val cls = javaClass<KlaxonTest>()
        return Parser2().parse(cls.getResourceAsStream(name)!!)
    }

    Test
    fun simple() {
        val j = read("/b.json")
        val expected = JsonArray()
                .add(1.toLong())
                .add("abc")
                .add(2.34)
                .add(false)
        assertEquals(expected, j)
    }

    Test
    fun basic() {
        val j = read("/a.json")
        val expected = JsonObject()
            .put("a", "b")
            .put("c", JsonArray()
                .add(1.toLong())
                .add(2.34)
                .add("abc")
                .add(false))
            .put("e", JsonObject()
                .put("f", 30.toLong())
                .put("g", 31.toLong()))

        assertEquals(expected, j)
    }
}

