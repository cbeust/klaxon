package com.beust.klaxon

import java.lang.reflect.Field

interface TypeConverter<T> {
    /**
     * Convert a value read in a JSON document into a Kotlin value.
     */
    fun fromJson(field: Field, value: JsonValue) : T?

    /**
     * Return a JSON document for the passed object.
     */
    fun toJson(obj: T) : String
}

