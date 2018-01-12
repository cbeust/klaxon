package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test

@Test
class JsonAnnotationTest {
    private val jsonString: String = json { obj(
            "name" to "John"
    ) }.toJsonString()!!

    fun ignoredWithAnnotation() {
        class IgnoredWithAnnotation(val name: String) {
            @Json(ignored = true)
            val change get(): Int = 0
        }

        val result = Klaxon().parse<IgnoredWithAnnotation>(jsonString)
        Assert.assertEquals(result?.name, "John")
    }

    fun ignoredWithPrivate() {
        class IgnoredWithPrivate(val name: String) {
            private val change get(): Int = 0
        }

        val result = Klaxon().parse<IgnoredWithPrivate>(jsonString)
        Assert.assertEquals(result?.name, "John")
    }
}