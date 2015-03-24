package com.beust.klaxon

import java.util.ArrayList
import java.util.LinkedHashMap

public fun valueToString(v: Any?) : String =
    StringBuilder {

        when (v) {
            is JsonBase -> v.appendJsonString(this)

            is String -> append("\"").append(v).append("\"")
            else -> append(v)
        }

    }.toString()

public trait JsonBase {
    fun appendJsonString(result : StringBuilder)
    fun toJsonString() : String = StringBuilder { appendJsonString(this) }.toString()
}

public fun JsonObject(map : Map<String, Any?> = emptyMap()) : JsonObject =
        JsonObject(LinkedHashMap(map))

public data class JsonObject(val map: MutableMap<String, Any?>) : JsonBase, Map<String, Any?> by map {

    fun put(key : String, value : Any?) {
        map[key] = value
    }

    override fun appendJsonString(result: StringBuilder) {
        result.append("{ ")

        var comma = false
        for ((k, v) in map) {
            if (comma) {
                result.append(", ")
            } else {
                comma = true
            }
            result.append("\"").append(k).append("\" : ")
            result.append(valueToString(v)).append(" ")
        }

        result.append("} ")
    }
}

[suppress("UNCHECKED_CAST")]
public fun <T> JsonObject.array(fieldName: String) : JsonArray<T>? = get(fieldName) as JsonArray<T>?
public fun JsonObject.obj(fieldName: String) : JsonObject? = get(fieldName) as JsonObject?
public fun JsonObject.long(fieldName: String) : Long? = get(fieldName) as Long?
public fun JsonObject.string(fieldName: String) : String? = get(fieldName) as String?
public fun JsonObject.double(fieldName: String) : Double? = get(fieldName) as Double?
public fun JsonObject.boolean(fieldName: String) : Boolean? = get(fieldName) as Boolean?


public fun <T> JsonArray(list : List<T> = emptyList()) : JsonArray<T> =
        JsonArray(list.toArrayList())

public data class JsonArray<T>(val value : MutableList<T>) : JsonBase, MutableList<T> by value {

    override fun appendJsonString(result: StringBuilder) {
        result.append("[ ")

        var comma = false
        value.forEach {
            if (comma) {
                result.append(", ")
            } else {
                comma = true
            }
            result.append(valueToString(it)).append(" ")
        }
        result.append("] ")
    }

}

public fun JsonArray<*>.string(id: String) : JsonArray<String?> = mapChildren { it.string(id) }

public fun JsonArray<*>.obj(id: String) : JsonArray<JsonObject?> = mapChildren { it.obj(id) }

public fun JsonArray<*>.long(id: String) : JsonArray<Long?> = mapChildren { it.long(id) }

public inline fun <T> JsonArray<*>.mapChildren(block : (JsonObject) -> T) : JsonArray<T> =
        JsonArray(mapTo(ArrayList(size())) {block(it as JsonObject) })