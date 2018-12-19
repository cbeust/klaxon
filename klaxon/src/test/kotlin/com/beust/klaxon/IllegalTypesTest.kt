package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.Assert
import org.testng.annotations.Test

class IllegalTypesTest {

    class TestObject

    @Test
    fun `illegal types inform you the type passed in`() {
        val testObject = TestObject()
        val exception =
            Assert.expectThrows(IllegalArgumentException::class.java) {
                json {
                    array(testObject)
                }
            }
        assertThat(exception.message)
            .contains(TestObject::class.java.name)
            .contains(testObject.toString())
    }
}