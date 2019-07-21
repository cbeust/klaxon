package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.math.BigDecimal

@Test
class Issue278Test {
    val BigDecimalConverter = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean = cls == BigDecimal::class.java

        override fun fromJson(jv: JsonValue): Any? {
            println("JsonValue = ${jv}")
            return BigDecimal.valueOf(jv.longValue!!)
        }

        override fun toJson(value: Any): String {
            TODO("not implemented")
        }
    }

    fun test() {
        data class Economy (
                val nationalDebt : BigDecimal
        )
        val expected = 9007199254740991
        val json = "{ \"nationalDebt\" : $expected }"
        val klaxon = Klaxon().converter(BigDecimalConverter)
        val obj = klaxon.parse<Economy>(json)
        assertThat(obj!!.nationalDebt).isEqualTo(BigDecimal.valueOf(expected))
    }
}