package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class Issue224Test {
    class Data1(val value: List<List<Double>>) {
        override fun toString() = "[${value.joinToString { "[${it.joinToString()}]" }}]"
    }

    class Data2(val value: List<List<List<Double>>>) {
        override fun toString() = "[${value.joinToString { "[${it.joinToString { "[${it.joinToString()}]" }}]" }}]"
    }

    fun issue224() {
        val klaxon = Klaxon()

        val input1 = Data1(listOf(listOf(1.0, 1.1, 1.2)))
        assertThat(input1.toString()).isEqualTo("[[1.0, 1.1, 1.2]]")

        val input2 = Data2(listOf(listOf(listOf(1.0, 1.1, 1.2))))
        assertThat(input2.toString()).isEqualTo("[[[1.0, 1.1, 1.2]]]")

        val json1 = klaxon.toJsonString(input1)
        assertThat(json1).isEqualTo("{\"value\" : [[1.0, 1.1, 1.2]]}")

        val json2 = klaxon.toJsonString(input2)
        assertThat(json2).isEqualTo("{\"value\" : [[[1.0, 1.1, 1.2]]]}")

        val output1 = klaxon.parse<Data1>(json1)
        assertThat(output1.toString()).isEqualTo("[[1.0, 1.1, 1.2]]")

        val output2 = klaxon.parse<Data2>(json2)
        assertThat(output2.toString()).isEqualTo("[[[1.0, 1.1, 1.2]]]")
    }
}