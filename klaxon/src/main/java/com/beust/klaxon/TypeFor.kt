package com.beust.klaxon

import kotlin.reflect.KClass

interface TypeAdapter<Output> where Output: Any {
    fun instantiate(type: Any): KClass<out Output>
}

@Target(AnnotationTarget.PROPERTY)
annotation class TypeFor(val field: String, val adapter: KClass<out TypeAdapter<*>>)
