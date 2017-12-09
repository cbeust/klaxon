package com.beust.klaxon

class JsonValue(value: Any?) {
    var obj: JsonObject? = null
    var array: JsonArray<*>? = null
    var string: String? = null
    var int: Int? = null
    var float: Float? = null
    var char: Char? = null
    var boolean: Boolean? = null

    init {
        when(value) {
            is JsonObject -> obj = value
            is JsonArray<*> -> array = value
            is String -> string = value
            is Int -> int = value
            is Float -> float = value
            is Char -> char = value
            is Boolean -> boolean = value
            else -> throw KlaxonException("Don't know how to interpret value $value")
        }
    }

}

interface KlaxonAdapter<T> {
    fun fromJson(value: JsonValue) : T
    fun toJson(o: T) : String
}
