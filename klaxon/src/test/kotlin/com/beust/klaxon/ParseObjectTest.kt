package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertEquals

@Test
class ParseObjectTest {

    object Foo

    fun `test parsing an object`() {
        assertEquals(Foo, Klaxon().parse<Foo>("{}"))
    }
}
