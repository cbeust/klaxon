package com.beust.klaxon

/**
 * Convert a custom type to and from JSON.
 */
interface Converter {
    /**
     * @return true if this converter can convert this class.
     */
    fun canConvert(cls: Class<*>) : Boolean

    /**
     * @return the JSON representation of the given value.
     */
    fun toJson(value: Any): String

    /**
     * Convert the given Json value into an object.
     */
    fun fromJson(jv: JsonValue) : Any?
}
