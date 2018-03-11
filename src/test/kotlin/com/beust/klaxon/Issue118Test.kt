package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.reflect.full.memberProperties

@Test
class Issue118Test {
    interface Foo{ val x: Int }
    data class FooImpl(override val x: Int): Foo
    data class BarImpl(val y: Int, val foo: FooImpl): Foo { // by foo {
            @Json(ignored = true)
            override val x = foo.x
    }

    fun test() {
        val bi = BarImpl(24, FooImpl(10))
        println("X: " + bi.x)
        val kc = BarImpl::class
//        val jc = kc.java
        println("Fields: " + kc.memberProperties.joinToString())
//        println("Declared fields: " + jc.declaredFields.joinToString())

        val originalJson = """{"foo" : {"x" : 1}, "y" : 1}"""
        val instance = Klaxon().parse<BarImpl>(originalJson)!!
        val newJson = Klaxon().toJsonString(instance)

        println(newJson) //prints {"foo" : {"x" : 1}, "x" : 1, "y" : 1}
        Klaxon().parse<BarImpl>(newJson)
    }
}

