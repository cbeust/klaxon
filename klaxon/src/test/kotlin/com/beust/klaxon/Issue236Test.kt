package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.math.BigDecimal

@Test
class Issue236Test {
    fun issue236Decimal() {
        data class Poke(
                var block: Long = 0L,
                var value: BigDecimal = BigDecimal.ZERO // Could be String/Long/Int/doesntmatter

        )

        val json = """
            [
      {
                "block": 7102460,
                "value": 114470000000000000000.0
       },
       {
                "block": 7102393,
                "value": 114455000000000000000.0
       }
]
        """.trimIndent()

        val poke = Klaxon().parseArray<Poke>(json)
        assertThat(poke!![0]).isEqualTo(Poke(7102460, BigDecimal(114470000000000000000.0)))
        assertThat(poke[1]).isEqualTo(Poke(7102393, BigDecimal(114455000000000000000.0)))
    }
}
