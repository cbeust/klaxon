package com.beust.klaxon

interface Converter {
    fun toJson(value: Any): String?
    fun fromJson(jv: JsonValue) : Any?
}

