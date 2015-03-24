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
            obj("a" to "b",
                "c" to array(1, 2.34, "abc", false),
                "e" to obj("f" to 30, "g" to 31)
            )
        }
        assertEquals(expected, j)
    }

    Test
    fun nullsParse() {
        assertEquals(json {
            array(1, null, obj(
                    "a" to 1,
                    "." to null
            ))
        }, read("/nulls.json"))
    }

    Test
    fun nullsDSL() {
        val j = json {
            obj(
                    "1" to null,
                    "2" to 2
            )
        }

        assertEquals("""{ "1" : null , "2" : 2 } """, j.toJsonString())
    }
}

