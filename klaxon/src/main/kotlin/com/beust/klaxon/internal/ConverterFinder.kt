package com.beust.klaxon.internal

import com.beust.klaxon.Converter
import kotlin.reflect.KProperty

interface ConverterFinder {
    fun findConverter(value: Any, prop: KProperty<*>? = null): Converter
}
