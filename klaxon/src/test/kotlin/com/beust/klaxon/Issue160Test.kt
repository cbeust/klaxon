package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

data class A (val a: Int)
data class Parse<T>(val a: T)

@Test(enabled = false)
class Issue160Test {

    fun issue160() {
        val result = Klaxon().parse<Parse<A>>("""{"a":{"a":1}}""")
        println(result!!.a.a)
        assertThat(result!!.a.a).isEqualTo(1)
        println(result)
    }
}