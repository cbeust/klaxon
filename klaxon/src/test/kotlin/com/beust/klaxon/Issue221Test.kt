package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Test
class Issue221Test {
    class WithDate constructor(val name: String, @ESDate val birth: LocalDate)

    @Target(AnnotationTarget.FIELD)
    annotation class ESDate

    private val dateConverter = object : Converter {
        override fun canConvert(cls: Class<*>) = cls == LocalDate::class.java
        override fun fromJson(jv: JsonValue) = throw KlaxonException("Couldn't parse date: ${jv.string}")
        override fun toJson(value: Any) = (value as LocalDate).format(DateTimeFormatter.ofPattern("Y/M/d"))
    }

    fun issue221() {
        val k = Klaxon().fieldConverter(ESDate::class, dateConverter)
        val s = k.toJsonString(WithDate("hha", LocalDate.of(2018, 11, 30)))
        assertThat(s).contains("2018/11/30")
    }
}
