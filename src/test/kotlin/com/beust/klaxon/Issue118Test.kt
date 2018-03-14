package com.beust.klaxon

import org.testng.annotations.Test

@Test
class Issue118Test {
    interface Foo{ val x: Int }
    data class FooImpl(override val x: Int): Foo
    data class BarImpl(val y: Int, private val foo: FooImpl): Foo { // by foo {
            @Json(ignored = true)
            override val x = foo.x
    }

    fun test() {
        val originalJson = """{"foo" : {"x" : 1}, "y" : 1}"""
        val instance = Klaxon().parse<BarImpl>(originalJson)!!
        val newJson = Klaxon().toJsonString(instance)

        Klaxon().parse<BarImpl>(newJson)
    }
}

