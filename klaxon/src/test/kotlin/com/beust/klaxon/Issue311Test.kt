
package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test


@Test(description = "Serialing Enum values with their own anonymous classes")
class Issue311Test {
    enum class ProtocolState {
        WAITING {
            override fun signal() = TALKING
        },
        TALKING {
            override fun signal() = WAITING
        };
        abstract fun signal(): ProtocolState
    }

    class Klass(val state: ProtocolState)

    fun issue311() {
        val klass = Klass(ProtocolState.WAITING)
        assertThat(Klaxon().toJsonString(klass)).isEqualTo("""{"state" : "WAITING"}""")
    }
}
