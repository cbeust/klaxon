package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test

data class Card(
        var value: Int? = null,
        var suit: String? = null
)
data class Deck1(
        var card: Card? = null,
        var cardCount: Int? = null
)

@Test
class BindingTest2 {

    //
    // Tests objects -> JSON string
    //

    data class ArrayHolder(var listOfInts: List<Int> = emptyList(),
            var listOfStrings : List<String> = emptyList(),
            var listOfBooleans: List<Boolean> = emptyList(),
            var string: String = "foo", var isTrue: Boolean = true, var isFalse: Boolean = false)

    fun arrayToJson() {
        val klaxon = Klaxon()
        val h = ArrayHolder(listOf(1, 3, 5),
                listOf("d", "e", "f"),
                listOf(true, false, true))
        val s2 = klaxon.toJsonString(h)
        Assert.assertTrue(s2.contains("\"listOfInts\" : [1, 3, 5]"))
        Assert.assertTrue(s2.contains("\"listOfStrings\" : [\"d\", \"e\", \"f\"]"))
        Assert.assertTrue(s2.contains("\"listOfBooleans\" : [true, false, true]"))
        Assert.assertTrue(s2.contains("\"string\" : \"foo\""))
        Assert.assertTrue(s2.contains("\"isTrue\" : true"))
        Assert.assertTrue(s2.contains("\"isFalse\" : false"))
    }

    val CARD_CONVERTER = object: Converter {
        override fun fromJson(jv: JsonValue): Any? {
            val suit = jv.obj?.string("suit")
            val value = jv.obj?.int("value")
            return if (suit != null && value != null) Card(value, suit) else null
        }

        override fun toJson(o: Any): String? {
            val result = if (o is Card) {
                """
                    "value" : ${o.value},
                    "suit": "${o.suit?.toUpperCase()}"
                """
            } else {
                null
            }
            return result
        }
    }

    fun objectsToJson() {
        val klaxon = Klaxon().converter(CARD_CONVERTER)

//        val deck = klaxon.parse<BindingTest.Deck1>("""
//            {
//                "cardCount": 1,
//                "card" : "KS"
//            }
//        """)

        val deck1 = Deck1(cardCount = 1, card = Card(13, "Clubs"))

        val s2 = klaxon.toJsonString(deck1)
        listOf("\"CLUBS\"", "\"suit\"", "\"value\"", "13", "\"cardCount\"", "1").forEach {
            Assert.assertTrue(s2.contains(it))
        }
    }

    //
    // Tests parsing
    //

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

    fun compoundObject() {
        val result = Klaxon()
                .converter(CARD_CONVERTER)
                .parse<Deck1>("""
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
        val result = Klaxon()
                .converter(CARD_CONVERTER)
                .parse<Deck2>("""
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
}