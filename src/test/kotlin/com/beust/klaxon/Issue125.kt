package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

/**
 * https://github.com/cbeust/klaxon/issues/125
 */
@Test
class Issue125 {
    open class Parent(val foo: String)
    class Child(@Json(ignored = false) foo: String, val bar: String) : Parent(foo)

    fun runTest() {
        val jsonString = """
        {
            "foo": "fofo" ,
            "bar": "baba"
        }
        """

        val parent = Klaxon().parse<Parent>(jsonString)
        assertThat(parent?.foo).isEqualTo("fofo")
        val child = Klaxon().parse<Child>(jsonString)
        assertThat(child?.foo).isEqualTo("fofo")
        assertThat(child?.bar).isEqualTo("baba")
    }

}