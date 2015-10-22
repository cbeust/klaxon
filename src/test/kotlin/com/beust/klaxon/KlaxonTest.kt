package com.beust.klaxon

import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@Test
class KlaxonTest {
    @BeforeClass
    fun bc() {
    }

    private fun read(name: String): Any? {
        val cls = KlaxonTest::class.java
        return Parser().parse(cls.getResourceAsStream(name)!!)
    }

    fun simple() {
        val j = read("/b.json")
        val expected = json {
            array(1, "abc", 2.34, false)
        }
        assertEquals(expected, j)
    }

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

    fun nullsParse() {
        assertEquals(json {
            array(1, null, obj(
                    "a" to 1,
                    "." to null
            ))
        }, read("/nulls.json"))
    }

    fun nullsDSL() {
        val j = json {
            obj(
                    "1" to null,
                    "2" to 2
            )
        }

        assertEquals("""{"1":null,"2":2}""", j.toJsonString())
    }

    fun prettyPrintObject() {
        val j = json {
            obj(
                    "a" to 1,
                    "b" to "text"
            )
        }

        val expected = """{
  "a": 1,
  "b": "text"
}"""
        val actual = j.toJsonString(true)
        assertEquals(trim(expected), trim(actual))
    }

    fun prettyPrintEmptyObject() {
        assertEquals("{}", JsonObject(emptyMap()).toJsonString(true))
    }

    fun prettyPrintArray() {
        assertEquals("[1, 2, 3]", JsonArray(1, 2, 3).toJsonString(true))
    }

    fun prettyPrintNestedObjects() {
        val expected = """{
  "a": 1,
  "obj": {
    "b": 2
  }
}"""

        val actual = json {
            obj(
                    "a" to 1,
                    "obj" to json {
                        obj("b" to 2)
                    }
            )
        }.toJsonString(true)

        assertEquals(trim(actual), trim(expected))
    }

    private fun trim(s: String) = s.replace("\n", "").replace("\r", "")

    fun renderStringEscapes() {
        assertEquals(""" "test\"it\n" """.trim(), valueToString("test\"it\n"))
    }

    fun parseStringEscapes() {
        assertEquals(json {
            obj("s" to "text field \"s\"\nnext line\u000cform feed\ttab\\rev solidus/solidus\bbackspace")
        }, read("/escaped.json"))
    }

    fun arrayLookup() {
        val j = json {
            array(
                    obj(
                            "nick" to "SuperMan",
                            "address" to obj("country" to "US"),
                            "weight" to 89.4,
                            "d" to 1L,
                            "i" to 4,
                            "b" to java.math.BigInteger("123456789123456789123456786")
                    ),
                    obj(
                            "nick" to "BlackOwl",
                            "address" to obj("country" to "UK"),
                            "weight" to 75.7,
                            "d" to 2L,
                            "i" to 3,
                            "b" to java.math.BigInteger("123456789123456789123456787")
                    ),
                    obj(
                            "nick" to "Anonymous",
                            "address" to null,
                            "weight" to -1.0,
                            "d" to 3L,
                            "i" to 2,
                            "b" to java.math.BigInteger("123456789123456789123456788")
                    ),
                    obj(
                            "nick" to "Rocket",
                            "address" to obj("country" to "Russia"),
                            "weight" to 72.0,
                            "d" to 4L,
                            "i" to 1,
                            "b" to java.math.BigInteger("123456789123456789123456789")
                    )
            )
        }

        assertEquals(listOf("SuperMan", "BlackOwl", "Anonymous", "Rocket"),
                j.string("nick").filterNotNull())
        assertEquals(listOf("US", "UK", null, "Russia"),
                j.obj("address").map { it?.string("country") })
        assertKlaxonEquals(JsonArray(89.4, 75.7, -1.0, 72.0), j.double("weight"))
        assertKlaxonEquals(JsonArray(1L, 2L, 3L, 4L), j.long("d"))
        assertKlaxonEquals(JsonArray(4, 3, 2, 1), j.int("i"))
        assertKlaxonEquals(JsonArray(
                java.math.BigInteger("123456789123456789123456786"),
                java.math.BigInteger("123456789123456789123456787"),
                java.math.BigInteger("123456789123456789123456788"),
                java.math.BigInteger("123456789123456789123456789")), j.bigInt("b"))
    }

    private fun <T> assertKlaxonEquals(expected: List<T>, actual: JsonArray<T>) {
        for (i in 0..expected.size - 1) {
            assertEquals(expected.get(i), actual.get(i))
        }
    }
    
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

    fun arrayFiltering() {
        val j = json {
            array(1, 2, 3, obj("a" to 1L))
        }

        assertEquals(listOf(1L), j.filterIsInstance<JsonObject>().map { it.long("a") })
    }

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

    fun lookupArray() {
        val j = json {
            array(
                    "yo", obj("a" to 1)
            )
        }

        assertEquals(JsonArray(null, 1), j.lookup("a"))
    }

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

    fun lookupSingleObject() {
        val j = json {
            obj("a" to 1)
        }

        assertEquals(1, j.lookup("a").single())
    }

    fun mapChildren() {
        val j = json {
            array(1,2,3)
        }

        val result = j.mapChildrenObjectsOnly { fail("should never reach here") }
        assertTrue(result.isEmpty())
    }

    fun mapChildrenWithNulls() {
        val j = json {
            array(1,2,3)
        }

        val result = j.mapChildren { fail("should never reach here") }
        assertKlaxonEquals(listOf(null, null, null), result)
    }
}
