package com.beust.klaxon

import org.testng.annotations.Test

data class TestObject(val a: Int, val b: String, val c: Float)

@Test
class Issue167Test {
    fun issue167() {
        val aJsonObject = json {
            obj("a" to 1, "b" to "value")
        }
        println("Json object: ${aJsonObject.toJsonString()}")

        val anArray = json {
            array("a", 1, false)
        }
        println("Json array: ${anArray.toJsonString()}")

        val aPlainObject = TestObject(10, "test string", 3.141659f)
        println("Plain object: ${aPlainObject.toString()}")

        val aMix = json {
            obj (
                "theArray" to anArray,
                "theObject" to aJsonObject,
                "thePlainObject" to aPlainObject,   // This entry would fail previously: IllegalArgumentException
                "anInt" to 4
            )
        }

        println("Mix: ${aMix.toJsonString()}")
    }
}
