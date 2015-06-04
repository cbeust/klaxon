package com.beust.klaxon

import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KlaxonTest {
    BeforeClass
    fun bc() {
    }

    private fun read(name: String): Any? {
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

        assertEquals("""{"1":null,"2":2}""", j.toJsonString())
    }

    Test
    fun prettyPrintObject() {
        val j = json {
            obj(
                    "a" to 1,
                    "b" to "text"
            )
        }

        assertEquals("""{
  "a": 1,
  "b": "text"
}""", j.toJsonString(true))
    }

    Test
    fun prettyPrintEmptyObject() {
        assertEquals("{}", JsonObject(emptyMap()).toJsonString(true))
    }

    Test
    fun prettyPrintArray() {
        assertEquals("[1, 2, 3]", JsonArray(1, 2, 3).toJsonString(true))
    }

    Test
    fun prettyPrintNestedObjects() {
        assertEquals("""{
  "a": 1,
  "obj": {
    "b": 2
  }
}""", json {
            obj(
                    "a" to 1,
                    "obj" to json {
                        obj("b" to 2)
                    }
            )
        }.toJsonString(true))
    }

    Test
    fun renderStringEscapes() {
        assertEquals(""" "test\"it\n" """.trim(), valueToString("test\"it\n"))
    }

    Test
    fun arrayLookup() {
        val j = json {
            array(
                    obj(
                            "nick" to "SuperMan",
                            "address" to obj("country" to "US"),
                            "weight" to 89.4,
                            "d" to 1L
                    ),
                    obj(
                            "nick" to "BlackOwl",
                            "address" to obj("country" to "UK"),
                            "weight" to 75.7,
                            "d" to 1L
                    ),
                    obj(
                            "nick" to "Anonymous",
                            "address" to null,
                            "weight" to -1.0,
                            "d" to 1L
                    ),
                    obj(
                            "nick" to "Rocket",
                            "address" to obj("country" to "Russia"),
                            "weight" to 72.0,
                            "d" to 1L
                    )
            )
        }

        assertEquals(listOf("SuperMan", "BlackOwl", "Anonymous", "Rocket"), j.string("nick").filterNotNull())
        assertEquals(listOf("US", "UK", null, "Russia"), j.obj("address").map { it?.string("country") })
        assertEquals(listOf(89.4, 75.7, -1.0, 72.0), j.double("weight"))
        assertEquals(listOf(1L, 1L, 1L, 1L), j.long("d"))
    }

    Test
    fun objectLookup() {
        val j = json {
            obj(
                    "nick" to "BlackOwl",
                    "address" to obj("country" to "UK"),
                    "weight" to 75.7,
                    "d" to 1L,
                    "true" to true
            )
        }

        assertEquals("BlackOwl", j.string("nick"))
        assertEquals(JsonObject(mapOf("country" to "UK")), j.obj("address"))
        assertEquals(75.7, j.double("weight"))
        assertEquals(1L, j.long("d"))
        assertTrue(j.boolean("true")!!)
    }

    Test
    fun arrayFiltering() {
        val j = json {
            array(1, 2, 3, obj("a" to 1L))
        }

        assertEquals(listOf(1L), j.filterIsInstance<JsonObject>().map { it.long("a") })
    }

    Test
    fun lookupObjects() {
        val j = json {
            obj(
                    "users" to
                            array(
                                    obj(
                                            "name" to "Sergey",
                                            "weight" to 65.0
                                    ),
                                    obj(
                                            "name" to "Bombshell",
                                            "weight" to 121.0
                                    ),
                                    null
                            )
            )
        }

        assertEquals(JsonArray("Sergey", "Bombshell", null), j.lookup("/users/name"))
        assertEquals(JsonArray("Sergey", "Bombshell", null), j.lookup("users.name"))
    }

    Test
    fun lookupArray() {
        val j = json {
            array(
                    "yo", obj("a" to 1)
            )
        }

        assertEquals(JsonArray(null, 1), j.lookup("a"))
    }

    Test
    fun lookupNestedArrays() {
        val j = json {
            array (
                    array(
                            array(
                                    "yo", obj("a" to 1)
                            )
                    )
            )
        }

        assertEquals(JsonArray(null, 1), j.lookup("a"))
    }

    Test
    fun lookupSingleObject() {
        val j = json {
            obj("a" to 1)
        }

        assertEquals(1, j.lookup("a").single())
    }
}

