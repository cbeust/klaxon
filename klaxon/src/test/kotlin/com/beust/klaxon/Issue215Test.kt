package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class Issue215Test {
    fun issue215() {
        val input = """{"hi" : "hello"}"""
        val map = Klaxon().parse<Map<String, String>>(input)
        assertThat(map).isEqualTo(mapOf("hi" to "hello"))
    }
}