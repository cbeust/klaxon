package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



@Test
class BindingTest {
    data class AllTypes constructor(var int: Int? = null,
            var string: String? = null,
            var isTrue: Boolean? = null,
            var isFalse: Boolean? = null,
            var array: List<Int> = emptyList())
    fun allTypes() {
        val result = Klaxon().parse<AllTypes>("""
        {
            "int": 42,
            "string": "foo",
            "isTrue": true,
            "isFalse": false,
            "array": [11, 12]
        }
        """)
        Assert.assertEquals(result, AllTypes(42, "foo", true, false, listOf(11, 12)))
    }

    data class Card(
        var value: Int? = null,
        var suit: String? = null
    )

    data class Deck1(
        var card: Card? = null,
        var cardCount: Int? = null
    )

    fun compoundObject() {
        val result = Klaxon().parse<Deck1>("""
        {
          "cardCount": 2,
          "card":
            {"value" : 5,"suit" : "Hearts"}
        }
        """)

        if (result != null) {
            Assert.assertEquals(result.cardCount, 2)
            val card = result.card
            if (card != null) {
                Assert.assertEquals(card, Card(5, "Hearts"))
            } else {
                Assert.fail("Should have received a non null card")
            }
        } else {
            Assert.fail("Should have received a non null deck")
        }
    }

    data class Deck2(
            var cards: List<Card> = emptyList(),
            var cardCount: Int? = null
    )

    fun compoundObjectWithArray() {
        val result = Klaxon().parse<Deck2>("""
        {
          "cardCount": 2,
          "cards": [
            {"value" : 5, "suit" : "Hearts"},
            {"value" : 8, "suit" : "Spades"},
          ]
        }
    """)

        if (result != null) {
            Assert.assertEquals(result.cardCount, 2)
            Assert.assertEquals(result.cards, listOf(Card(5, "Hearts"), Card(8, "Spades")))
        } else {
            Assert.fail("Should have received a non null deck")
        }
    }

    class BadMapping @JvmOverloads constructor(
            var badName: String? = null
    )

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun badFieldMapping2() {
        Klaxon().parse<BadMapping>("""
        {
          "goodName": "foo"
        }
        """)
    }

    class Mapping @JvmOverloads constructor(
            @field:Json(name = "theName")
            var name: String? = null
    )

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun badFieldMapping() {
        Klaxon().parse<Mapping>("""
        {
          "name": "foo"
        }
        """)
    }

    fun goodFieldMapping() {
        val result = Klaxon().parse<Mapping>("""
        {
          "theName": "foo"
        }
        """)
        Assert.assertEquals(result?.name, "foo")
    }

    data class WithDate(
            @field:Json(name = "theDate")
            @field:KlaxonDate
            var date: LocalDateTime? = null,

            @field:KlaxonDayOfTheWeek
            var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    fun fieldAdapters() {
        val result = Klaxon()
            .fieldConverter(KlaxonDate::class, object: TypeConverter<LocalDateTime?> {
                override fun fromJson(field: Field, value: JsonValue)
                        = LocalDateTime.parse(value.string,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                override fun toJson(o: LocalDateTime?): String {
                    return """ {
                    | "date" : ${o?.toString()} }
                    | """.trimMargin()
                }
            })

            .fieldConverter(KlaxonDayOfTheWeek::class, object: TypeConverter<String> {
                override fun fromJson(field: Field, value: JsonValue) : String {
                    return when(value.int) {
                        0 -> "Sunday"
                        1 -> "Monday"
                        2 -> "Tuesday"
                        else -> "Some other day"
                    }
                }

                override fun toJson(o: String) : String {
                    return when(o) {
                        "Sunday" -> "0"
                        "Monday" -> "1"
                        "Tuesday" -> "2"
                        else -> "-1"
                    }
                }
            })
            .parse<WithDate>("""
                {
                  "theDate": "2017-05-10 16:30"
                  "dayOfTheWeek": 2
                }
            """)
        Assert.assertEquals(result?.dayOfTheWeek, "Tuesday")
        Assert.assertEquals(result?.date, LocalDateTime.of(2017, 5, 10, 16, 30))
    }

    val CARD_ADAPTER = object: TypeConverter<Card?> {
        override fun fromJson(field: Field, value: JsonValue): Card? {
            fun parseCard(str: String) : Card? {
                val s0 = str[0]
                val cardValue =
                    if (s0 == '1' && str[1] == '0') 10
                    else if (s0 == 'K') 13
                    else (s0 - '0')
                val suit = when(str[1]) {
                    'H' -> "Hearts"
                    'S' -> "Spades"
                    else -> ""
                }
                return if (suit != "") Card(cardValue, suit) else null
            }
            val str = value.string
            return if (str != null) parseCard(str) else null
        }

        override fun toJson(o: Card?): String {
            return "some JSON"
        }
    }

    private fun privateTypeConverter(withAdapter: Boolean) {
        val klaxon = Klaxon()
        if (withAdapter) klaxon.typeConverter(CARD_ADAPTER)
        val result = klaxon.parse<Deck1>("""
            {
                "cardCount": 1,
                "card" : "KS"
            }
        """)
        Assert.assertEquals(result?.cardCount, 1)
        Assert.assertEquals(result?.card, Card(13, "Spades"))
    }

    fun withTypeConverter() = privateTypeConverter(withAdapter = true)

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun withoutTypeConverter() = privateTypeConverter(withAdapter = false)
}

annotation class KlaxonDate
annotation class KlaxonDayOfTheWeek
