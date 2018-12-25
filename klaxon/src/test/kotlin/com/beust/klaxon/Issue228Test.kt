package com.beust.klaxon

import org.testng.annotations.Test
import java.math.BigDecimal

@Test
class Issue228Test {
    fun issue228() {
        data class CurrencySnapshot(val rates: Map<String, BigDecimal>)

        val parsed = Klaxon().parse<CurrencySnapshot>("{\"rates\":{\"EUR\":1,\"FJD\":2.434077,}}")
        listOf("FJD", "EUR").forEach {
            val v = parsed!!.rates[it]!!
            if (v !is BigDecimal) throw AssertionError("Field $it is not of type BigDecimal: " + v::class)
        }
    }

}