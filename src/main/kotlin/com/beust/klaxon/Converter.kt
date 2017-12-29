package com.beust.klaxon

import kotlin.reflect.KProperty

interface Converter {
    fun toJson(value: Any): String?
    fun fromJson(jv: JsonValue) : Any?
}
