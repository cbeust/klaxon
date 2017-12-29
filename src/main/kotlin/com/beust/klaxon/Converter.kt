package com.beust.klaxon

interface Converter {
    fun toJson(o: Any): String?
    fun fromJson(jv: JsonValue) : Any?
}

