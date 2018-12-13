package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

/**
 * https://github.com/cbeust/klaxon/issues/125
 */
@Test
class Issue125Test {
    open class Parent(open val foo: String)
    class Child(@Json(ignored = false) override val foo: String, val bar: String) : Parent(foo)

    fun runTest() {
        val jsonString = """
        {
            "foo": "fofo" ,
            "bar": "baba"
        }
        """

        val parent = Klaxon().parse<Parent>(jsonString)
        assertThat(parent?.foo).isEqualTo("fofo")
//        val child = Klaxon().parse<Child>(jsonString)
//        assertThat(child?.foo).isEqualTo("fofo")
//        assertThat(child?.bar).isEqualTo("baba")
    }

    @Test(enabled = false, description = "List of maps not supported yet")
    fun objectWithListOfMaps() {
        val mapper = Klaxon()
        data class Data(val data: List<Map<String, String>>)

        val data = Data(listOf(mapOf("name" to "john")))
        val json = mapper.toJsonString(data)
        assertThat(mapper.parse<Data>(json)).isEqualTo(data)
    }
}
