package com.beust.klaxon.internal

import com.beust.klaxon.Converter
import com.beust.klaxon.Property1

interface ConverterFinder {
    fun findConverter(value: Any, prop: Property1? = null): Converter
}
