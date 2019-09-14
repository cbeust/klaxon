package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class Issue267Test {
    fun issue167() {
        class MyClass(i: Int, s: String, listOf: List<String>)
        val myObject = MyClass(1, "two", listOf())
        val json = json {
            obj("myobject" to myObject)
        }
        assertThat(json.toJsonString()).isEqualTo("""{"myobject":{}}""")
    }

    fun embed() {
        class MyClass(val i: Int, s: String, listOf: List<String>)
        val myObject = MyClass(1, "two", listOf())
        val json = json {
            obj("myobject" to myObject)
        }
        assertThat(json.toJsonString()).isEqualTo("""{"myobject":{"i" : 1}}""")
    }
}