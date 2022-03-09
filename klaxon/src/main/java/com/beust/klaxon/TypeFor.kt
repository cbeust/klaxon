package com.beust.klaxon

import kotlin.reflect.KClass

interface TypeAdapter<Output> where Output: Any {
    fun classFor(type: Any): KClass<out Output>
    fun classForNullable(type: Any?): KClass<out Output>{
        return classFor(type as Any)
    }
}

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class TypeFor(val field: String, val adapter: KClass<out TypeAdapter<*>>)
