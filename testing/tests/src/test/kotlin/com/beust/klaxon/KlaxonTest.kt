package com.beust.klaxon

import com.beust.klaxon.jackson.jackson
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.testng.Assert
import org.testng.annotations.Test
import java.math.BigDecimal
import java.util.Collections.emptyMap
import java.util.regex.Pattern
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@Test
class DefaultParserTest : KlaxonBaseTest() {
    override fun provideParser(): Parser =
        Parser.default()
}

@Test
class JacksonParserTest : KlaxonBaseTest() {
    override fun provideParser(): Parser =
        Parser.jackson()
}

@Test
abstract class KlaxonBaseTest {

    protected abstract fun provideParser(): Parser

    private fun read(name: String): Any? {
        val cls = KlaxonBaseTest::class.java
        return provideParser().parse(cls.getResourceAsStream(name)!!)
    }

    fun generated() {
        @Suppress("UNCHECKED_CAST")
        val j = read("/generated.json") as JsonArray<JsonObject>
        Assert.assertEquals((j[0]["name"] as JsonObject)["last"], "Olson")
    }

    fun simple() {
        val j = read("/b.json")
        val expected = json {
            array(1, "abc", false)
        }
        assertEquals(expected, j)
    }

    fun basic() {
        val j = read("/a.json")
        val expected = json {
            obj("a" to "b",
                    "c" to array(1, "abc", false),
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

    fun nullsDsl() {
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

	fun canonicalJsonObject() {
		val j = json {
            obj(
                    "c" to 1,
                    "a" to 2,
                    "b" to obj(
                            "e" to 1,
                            "d" to 2
                    )
            )
		}.toJsonString(canonical = true)

		val expected = """{"a":2,"b":{"d":2,"e":1},"c":1}"""

		assertEquals(j, expected)
	}

    fun canonicalJsonNumber() {
        val j = json {
            obj(
                    "d" to 123456789.123456789,
                    "f" to 123456789.123456789f
            )
        }.toJsonString(canonical = true)

        assert(Pattern.matches("\\{(\"[a-z]+\":\\d\\.\\d+E\\d+(,|}\$))+", j))
    }

    private fun trim(s: String) = s.replace("\n", "").replace("\r", "")

    fun renderStringEscapes() {
        assertEquals(""" "test\"it\n" """.trim(), valueToString("test\"it\n"))
    }

    fun parseStringEscapes() {
        val s = "text field \"s\"\nnext line\u000cform feed\ttab\\rev solidus/solidus\bbackspace\u2018"
        assertEquals(json {
            obj(s to s)
        }, read("/escaped.json"))
    }

    fun issue91WithQuotedDoubleStrings() {
        val map = HashMap<String, String>()
        map["whoops"] = """ Hello "world" """
        val s = Klaxon().toJsonString(map)
        assertThat(s).contains("\\\"world\\\"")
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

        assertEquals(JsonArray("Sergey", "Bombshell", null), j.lookup<String?>("/users/name"))
        assertEquals(JsonArray("Sergey", "Bombshell", null), j.lookup<String?>("users.name"))
    }

    fun lookupArray() {
        val j = json {
            array(
                    "yo", obj("a" to 1)
            )
        }

        assertEquals(JsonArray(null, 1), j.lookup<Int?>("a"))
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

        assertEquals(JsonArray(null, 1), j.lookup<Int?>("a"))
    }

    fun lookupSingleObject() {
        val j = json {
            obj("a" to 1)
        }

        assertEquals(1, j.lookup<Int?>("a").single())
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

    private fun valueToString(v: Any?, prettyPrint: Boolean = false, canonical : Boolean = false) : String =
        StringBuilder().apply {
            Render.renderValue(v, this, prettyPrint, canonical, 0)
        }.toString()

    fun renderMap() {
        val map = mapOf(
                "a" to 1,
                "b" to "x",
                "c" to null
        )

        assertEquals(valueToString(map), "{\"a\":1,\"b\":\"x\",\"c\":null}")
    }

    fun renderList() {
        val list = listOf(null, 1, true, false, "a")

        assertEquals(valueToString(list), "[null,1,true,false,\"a\"]")
    }



    data class StockEntry(
            val date: String,
            val close: Double,
            val volume: Int,
            val open: Double,
            val high: Double,
            val low: Double
    )

    fun issue77() {
        val json = """
            [
          {
            "date": "2018/01/10",
            "close": 0.25,
            "volume": 500000,
            "open": 0.5,
            "high": 0.5,
            "low": 0.25
          }
        ]
        """
        Klaxon().parseArray<StockEntry>(json)
    }

    class PersonWitCity(val name: String, val city: City) {
        class City(val name: String)
    }

    fun arrayParse() {
        data class Child(val id: Int, val name: String)
        data class Parent(val children: Array<Child>)

        val array = """{
            "children":[
                {"id": 1, "name": "foo"},
                {"id": 2, "name": "bar"}
            ]
         }"""

        val r = Klaxon().parse<Parent>(array)
        with(r!!.children) {
            assertThat(this[0]).isEqualTo(Child(1, "foo"))
            assertThat(this[1]).isEqualTo(Child(2, "bar"))
        }
    }

    fun nestedCollections() {
        data class Root (val lists: List<List<String>>)

        val result = Klaxon().parse<Root>("""
        {
            "lists": [["red", "green", "blue"]]
        }
        """)
        assertThat(result).isEqualTo(Root(listOf(listOf("red", "green", "blue"))))
    }

    fun nested() {
        val r = Klaxon().parse<PersonWitCity>("""{
            "name": "John",
            "city": {
                "name": "San Francisco"
            }
        }""")
        Assert.assertEquals(r?.name, "John")
        Assert.assertEquals(r?.city?.name, "San Francisco")
    }

    fun bigDecimal() {
        data class A(val data: BigDecimal)
        val something = Klaxon().parse<A>("""
            {"data": 0.00000001}
            """)
        assertThat(BigDecimal(0.00000001).compareTo(BigDecimal(something!!.data.toDouble()))).isEqualTo(0)
    }

    enum class Colour { Red, Green, Blue }

    fun serializeEnum() {
        Assert.assertEquals(Klaxon().toJsonString(Colour.Red), "\"Red\"")
    }

    @Test
    fun nonConstructorProperties() {
        val result = Klaxon().parse<Registry>(someString)
        val vendors = result?.vendor!!
        assertThat("example").isEqualTo(result.name)
        assertThat("example").isEqualTo(vendors[0].vendorName)
    }

    private class Vendor {
        var vendorName : String = ""
    }

    @Language("json")
    private val someString = """{
        "name": "example",
        "foo": "cool",
        "boo": "stuff",
        "vendor": [
          { "vendorName": "example"}
        ]
    }"""


    @Test
    fun testParseRegistry() {
        val result = Klaxon().parse<Registry>(someString)
        val vendors = result?.vendor!!
        assertEquals("example", result!!.name)
        assertEquals("cool", result.foo)
        assertEquals("stuff", result.boo)
        assertEquals("example", vendors[0].vendorName)
    }
    private class Registry(val name : String,
            val vendor : List<Vendor> = ArrayList()) {
        var foo : String = ""
        var boo : String = ""
    }

    fun issue153() {
        abstract class FooBase(
                val id: String? = null
        )

        class BarImpl(
                val barValue: String? = null
        ): FooBase()


        val barImpl = Klaxon()
                .parse<BarImpl>("""
                {
                    "id": "id123",
                    "barValue" : "value123"
                }
                """)

        assertThat(barImpl?.barValue).isEqualTo("value123")
        assertThat(barImpl?.id).isEqualTo("id123")
    }
}
