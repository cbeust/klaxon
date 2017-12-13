package com.beust.klaxon

import kotlin.reflect.full.declaredMemberProperties

/**
 * Variant class that encapsulates one JSON value.
 */
class JsonValue(value: Any?, val jsonConverter: JsonConverter) {
    var obj: JsonObject? = null
    var array: JsonArray<*>? = null
    var string: String? = null
    var int: Int? = null
    var float: Float? = null
    var char: Char? = null
    var boolean: Boolean? = null

    var type: Class<*>

    init {
        when(value) {
            is JsonObject -> {
                obj = value
                type = value.javaClass
            }
            is JsonArray<*> -> {
                array = value
                type = List::class.java
            }
            is String -> {
                string = value
                type = String::class.java
            }
            is Int -> {
                int = value
                type = Int::class.java
            }
            is Float -> {
                float = value
                type = Float::class.java
            }
            is Char -> {
                char = value
                type = Char::class.java
            }
            is Boolean -> {
                boolean = value
                type = Boolean::class.java
            }
            else -> {
                obj = convertToJsonObject(value!!)
                type = value.javaClass
            }
        }
    }

    private fun convertToJsonObject(obj: Any): JsonObject {
        val result = JsonObject()
        obj::class.declaredMemberProperties.forEach { property ->
            println("Found property: " + property)
            val p = property.getter.call(obj)
            val converter = jsonConverter.findBestConverter(p)
            result[property.name] = converter?.fromJson(null, JsonValue(p, jsonConverter))
            println("  Converted: $converter")
        }
        return result
    }

    val inside: Any
        get() {
            val result = if (obj != null) obj
            else if (array != null) array
            else if (string != null) string
            else if (int != null) int
            else if (float != null) float
            else if (char != null) char
            else if (boolean != null) boolean
            else throw KlaxonException("Should never happen")
            return result!!
        }
}