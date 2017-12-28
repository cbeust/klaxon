package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


@Test
class BindingTest {

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

    class WithDate @JvmOverloads constructor(
            @Json(name = "theDate")
            @KlaxonDate
            var date: LocalDateTime? = null,

            @field:KlaxonDayOfTheWeek
            var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
    )

    fun fieldAdapters() {
        val result = Klaxon()
            .fieldConverter(KlaxonDate::class, object: Converter2 {
//                override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//                    return field?.returnType == LocalDateTime::class
//                            || value::class == LocalDateTime::class
//                }

                override fun fromJson(field: KProperty<*>?, value: JsonValue)
                    = LocalDateTime.parse(value.string,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                override fun toJson(o: Any): String {
                    return """ {
                    | "date" : ${o?.toString()} }
                    | """.trimMargin()
                }
            })

            .fieldConverter(KlaxonDayOfTheWeek::class, object: Converter2 {
//                override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//                    return field?.returnType == String::class
//                            || value::class == String::class
//                }

                override fun fromJson(field: KProperty<*>?, value: JsonValue) : String {
                    return when(value.int) {
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
            .parse<WithDate>("""
                {
                  "theDate": "2017-05-10 16:30"
                  "dayOfTheWeek": 2
                }
            """)
        Assert.assertEquals(result?.dayOfTheWeek, "Tuesday")
        Assert.assertEquals(result?.date, LocalDateTime.of(2017, 5, 10, 16, 30))
    }

    val CARD_ADAPTER = object: Converter2 {
//        override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//            val jc = field?.returnType?.javaType as? Class<*>
//            val isCard = jc?.isAssignableFrom(Card::class.java) ?: false
//            return isCard && value is String
//        }

        override fun fromJson(field: KProperty<*>?, value: JsonValue): Card? {
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

    fun withConverter2() = privateConverter2(withAdapter = true)

    @Test(expectedExceptions = arrayOf(KlaxonException::class))
    fun withoutConverter2() = privateConverter2(withAdapter = false)

    data class ArrayHolder(var listOfInts: List<Int> = emptyList(),
            var listOfStrings : List<String> = emptyList(),
            var listOfBooleans: List<Boolean> = emptyList(),
            var string: String = "foo", var isTrue: Boolean = true, var isFalse: Boolean = false)
}

@Target(allowedTargets = AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class KlaxonDate
annotation class KlaxonDayOfTheWeek

class Ann @JvmOverloads constructor(
        @field:Json(name = "theName")
        var name: String? = null
)

class Ann2 @JvmOverloads constructor (
        @field:Json(name = "theDate")
        @field:KlaxonDate
        var date: LocalDateTime? = null,

        @field:KlaxonDayOfTheWeek
        var dayOfTheWeek: String? = null // 0 = Sunday, 1 = Monday, ...
)

fun main(args: Array<String>) {
    run {
        val prop = Ann2::class.memberProperties.firstOrNull { it.name == "date" }
        if (prop != null) {
            val field = Ann2::class.java.getDeclaredField("date")
            val ann2 = field.annotations
            println("Annotations: " + ann2.size)
            println("")
        }
    }
    run {
        val prop = Ann::class.memberProperties.firstOrNull { it.name == "name" }
        if (prop != null) {
            val field = Ann::class.java.getDeclaredField("name")
            val ann2 = field.annotations
            println("Annotations: " + ann2.size)
            println("")
        }
    }
}