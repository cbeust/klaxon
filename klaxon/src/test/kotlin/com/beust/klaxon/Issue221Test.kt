package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.text.SimpleDateFormat
import java.util.*

@Test
class Issue221Test {
    class WithDate @JvmOverloads constructor(val name: String, @ESDate val birth: Date)


    val dateConverter = object : Converter {
        val format = SimpleDateFormat("yyyy/MM/dd")

        override fun canConvert(cls: Class<*>): Boolean {
            return cls == Date::class.java
        }

        override fun fromJson(jv: JsonValue) =
                throw KlaxonException("Couldn't parse date: ${jv.string}")

        override fun toJson(o: Any): String {
            return format.format(o as Date)
        }
    }

    fun test() {
        val k = Klaxon()
                .fieldConverter(ESDate::class, dateConverter)
        val s = k.toJsonString(WithDate("hha", Date()))
        assertThat(s).contains("2018/11/30")
    }


    @Target(AnnotationTarget.FIELD)
    annotation class ESDate
}
