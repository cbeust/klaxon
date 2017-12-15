package com.beust.klaxon

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
    var genericType: Class<*>? = null

    var type: Class<*>

    init {
        when(value) {
            is JsonValue -> {
                println("PROBLEM")
                type = String::class.java
            }
            is JsonObject -> {
                obj = value
                type = value.javaClass
            }
            is Collection<*> -> {
                val v = JsonArray<Any>()
                genericType = null
                value.forEach {
                    if (it is Any) {
                        v.add(it)
                        genericType = it.javaClass
                    } else {
                        throw KlaxonException("Need to extract inside")
                    }
                }
                array = v
                type = List::class.java
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
                if (value == null) {
                    throw IllegalArgumentException("Should never be null")
                } else {
                    obj = convertToJsonObject(value)
                    type = value.javaClass
                }
            }
        }
    }

    private fun convertToJsonObject(obj: Any): JsonObject {
        val result = JsonObject()
        val kv = Klaxon2.propertiesAndValues(obj).entries.forEach { entry ->
            val property = entry.key
            val p = entry.value
            println("Found property: " + property)
            val converter = jsonConverter.findBestConverter(p)
            val jv = JsonValue(p, jsonConverter)
            result[property.name] = converter?.fromJson(null, jv)
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