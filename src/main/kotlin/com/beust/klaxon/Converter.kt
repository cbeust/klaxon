package com.beust.klaxon

interface Converter<T> {
    fun toJson(value: T): String?
    fun fromJson(jv: JsonValue) : T
}
