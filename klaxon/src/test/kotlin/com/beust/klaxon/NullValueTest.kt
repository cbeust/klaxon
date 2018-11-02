package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class NullValueTest {
    fun nullStrings() {
        assertThat(Klaxon().toJsonString(listOf(1, 2, null, null, 3)))
                .isEqualTo("[1, 2, null, null, 3]")
    }
}
