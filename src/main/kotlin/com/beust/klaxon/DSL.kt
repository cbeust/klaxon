package com.beust.klaxon

import java.util.ArrayList
import java.util.Arrays
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

public fun <T> JsonArray(vararg items : T) : JsonArray<T> =
    JsonArray(ArrayList(Arrays.asList(*items)))

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
