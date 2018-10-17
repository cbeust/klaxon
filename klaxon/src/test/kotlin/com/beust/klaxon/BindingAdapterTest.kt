package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
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
    class WrongFieldAdapter(
        @KlaxonDate
        var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    @Test
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
            Assert.assertTrue(ex.message?.contains("Couldn't parse") ?: false)
        }
    }

    class WithDate(
        @Json(name = "theDate")
        @KlaxonDate
        var date: LocalDateTime? = null,

        @KlaxonDayOfTheWeek
        var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    private fun createKlaxon()
        = Klaxon()
            .fieldConverter(KlaxonDate::class, object: Converter {
                override fun canConvert(cls: Class<*>) = cls == LocalDateTime::class.java

                override fun fromJson(jv: JsonValue) =
                    if (jv.string != null) {
                        LocalDateTime.parse(jv.string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    } else {
                        throw KlaxonException("Couldn't parse date: ${jv.string}")
                    }

                override fun toJson(o: Any)
                        = """ { "date" : $o } """
            })

            .fieldConverter(KlaxonDayOfTheWeek::class, object: Converter {
                override fun canConvert(cls: Class<*>) = cls == String::class.java
                override fun fromJson(jv: JsonValue) : String {
                    return when(jv.int) {
                        0 -> "Sunday"
                        1 -> "Monday"
                        2 -> "Tuesday"
                        else -> "Some other day"
                    }
                }

                override fun toJson(o: Any) : String {
                    return when(o.toString()) {
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

    companion object {
        val CARD_ADAPTER = object : Converter {
            override fun canConvert(cls: Class<*>) = cls == Card::class.java

            override fun fromJson(jv: JsonValue): Card {
                fun parseCard(str: String): Card? {
                    val s0 = str[0]
                    val cardValue =
                            if (s0 == '1' && str[1] == '0') 10
                            else if (s0 == 'K') 13
                            else (s0 - '0')
                    val suit = when (str[1]) {
                        'H' -> "Hearts"
                        'S' -> "Spades"
                        else -> ""
                    }
                    return if (suit != "") Card(cardValue, suit) else null
                }

                val result =
                        if (jv.string != null) {
                            val str = jv.string
                            if (str != null) parseCard(str) else null
                        } else {
                            null
                        }
                return result ?: throw KlaxonException("Couldn't parse card")
            }

            override fun toJson(value: Any): String {
                return "some JSON"
            }
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
        assertThat(result?.cardCount).isEqualTo(1)
        assertThat(result?.card).isEqualTo(Card(13, "Spades"))
    }

    fun withConverter2() = privateConverter2(withAdapter = true)

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun withoutConverter2() = privateConverter2(withAdapter = false)

    class Person(var fullName: String? = null)
    fun personMappingTest() {

        val result = Klaxon()
            .converter(object: Converter {
                override fun canConvert(cls: Class<*>) = cls == Person::class.java
                override fun toJson(value: Any): String {
                    return """{"fullName" : "${(value as Person).fullName}""""
                }

                override fun fromJson(jv: JsonValue)
                        = Person(jv.objString("firstName") + " " + jv.objString("lastName"))

            })
            .parse<Person>("""
            {
                "firstName": "John",
                "lastName": "Smith"
            }
        """)
        assertThat(result?.fullName).isEqualTo("John Smith")
    }

    class BooleanHolder(var flag: Boolean? = null)
    fun booleanConverter() {
        val result = Klaxon()
            .converter(object: Converter {
                override fun canConvert(cls: Class<*>) = cls == BooleanHolder::class.java

                override fun toJson(value: Any): String {
                    return """{"flag" : "${if ((value as BooleanHolder).flag == true) 1 else 0}""""
                }

                override fun fromJson(jv: JsonValue)
                    = BooleanHolder(jv.objInt("flag") != 0)

            })
            .parseArray<BooleanHolder>("""[
            { "flag": 1 }, { "flag": 0 }
            ]
        """)
        assertThat(result?.get(0)?.flag).isTrue()
        assertThat(result?.get(1)?.flag).isFalse()
    }
}