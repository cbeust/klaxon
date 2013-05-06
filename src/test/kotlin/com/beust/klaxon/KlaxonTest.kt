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
        return Parser().parse(cls.getResourceAsStream(name)!!)
    }

    Test
    fun simple() {
        val j = read("/b.json")
        val expected = json {
            array(1, "abc", 2.34, false)
        }
        assertEquals(expected, j)
    }

    Test
    fun basic() {
        val j = read("/a.json")
        val expected = json {
            obj("a", "b",
                "c", array(1, 2.34, "abc", false),
                "e", obj("f", 30, "g", 31)
            )
        }
        assertEquals(expected, j)
    }
}

