package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
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

    fun typeAdapters() {
        val result = Klaxon()
            .typeAdapter(KlaxonDate::class, object: KlaxonAdapter<LocalDateTime?> {
                override fun fromJson(value: JsonValue)
                        = LocalDateTime.parse(value.string,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                override fun toJson(o: LocalDateTime?): String {
                    return """ {
                    | "date" : ${o?.toString()} }
                    | """.trimMargin()
                }
            })

            .typeAdapter(KlaxonDayOfTheWeek::class, object: KlaxonAdapter<String> {
                override fun fromJson(value: JsonValue) : String {
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
}

annotation class KlaxonDate
annotation class KlaxonDayOfTheWeek
