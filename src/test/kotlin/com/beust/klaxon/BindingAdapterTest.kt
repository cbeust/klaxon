package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate
@Target(AnnotationTarget.FIELD)
annotation class KlaxonDayOfTheWeek

@Test
class BindingAdapterTest {
    class WrongFieldAdapter @JvmOverloads constructor (
        @KlaxonDate
        var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    @Test(expectedExceptionsMessageRegExp = "convert")
    fun wrongFieldAdapter() {
        try {
            val result = createKlaxon()
                    .parse<WrongFieldAdapter>("""
                {
                  "dayOfTheWeek": 2
                }
            """)
            Assert.fail("Should have been unable to convert")
        } catch(ex: Exception) {
            Assert.assertTrue(ex.message?.contains("not able to convert") ?: false)
        }
    }

    class WithDate @JvmOverloads constructor(
        @Json(name = "theDate")
        @KlaxonDate
        var date: LocalDateTime? = null,

        @KlaxonDayOfTheWeek
        var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    private fun createKlaxon()
        = Klaxon()
            .fieldConverter(KlaxonDate::class, object: Converter {
                override fun fromJson(jv: JsonValue) = if (jv.string != null) {
                        LocalDateTime.parse(jv.string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    } else {
                        null
                    }

                override fun toJson(o: Any)
                        = """ { "date" : $o } """.trimMargin()
            })

            .fieldConverter(KlaxonDayOfTheWeek::class, object: Converter {
                override fun fromJson(jv: JsonValue) : String {
                    return when(jv.int) {
                        0 -> "Sunday"
                        1 -> "Monday"
                        2 -> "Tuesday"
                        else -> "Some other day"
                    }
                }

                override fun toJson(o: Any) : String {
                    return when(o) {
                        "Sunday" -> "0"
                        "Monday" -> "1"
                        "Tuesday" -> "2"
                        else -> "-1"
                    }
                }
            })

    fun fieldAdapters() {
        val result = createKlaxon()
            .parse<WithDate>("""
            {
              "theDate": "2017-05-10 16:30"
              "dayOfTheWeek": 2
            }
        """)
        Assert.assertEquals(result?.dayOfTheWeek, "Tuesday")
        Assert.assertEquals(result?.date, LocalDateTime.of(2017, 5, 10, 16, 30))
    }

    val CARD_ADAPTER = object: Converter {

        override fun fromJson(value: JsonValue): Card? {
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
            val result =
                    if (value.string != null) {
                        val str = value.string
                        if (str != null) parseCard(str) else null
                    } else {
                        null
                    }
            return result
        }

        override fun toJson(obj: Any): String {
            return "some JSON"
        }
    }

    private fun privateConverter2(withAdapter: Boolean) {
        val klaxon = Klaxon()
        if (withAdapter) klaxon.converter(CARD_ADAPTER)
        val result = klaxon.parse<Deck1>("""
            {
                "cardCount": 1,
                "card" : "KS"
            }
        """)
        Assert.assertEquals(result?.cardCount, 1)
        Assert.assertEquals(result?.card, Card(13, "Spades"))
    }

    fun withConverter2() = privateConverter2(withAdapter = true)

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun withoutConverter2() = privateConverter2(withAdapter = false)

}