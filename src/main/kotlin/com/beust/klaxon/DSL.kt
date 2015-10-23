package com.beust.klaxon

import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedHashMap

public fun valueToString(v: Any?, prettyPrint: Boolean = false) : String =
    StringBuilder().apply {
        renderValue(v, this, prettyPrint, 0)
    }.toString()

public interface JsonBase {
    fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, level: Int)
    fun appendJsonString(result : Appendable, prettyPrint: Boolean = false) =
            appendJsonStringImpl(result, prettyPrint, 0)
    fun toJsonString(prettyPrint: Boolean = false) : String =
            StringBuilder().apply { appendJsonString(this, prettyPrint) }.toString()
}

public fun JsonObject(map : Map<String, Any?> = emptyMap()) : JsonObject =
        JsonObject(LinkedHashMap(map))

public data class JsonObject(val map: MutableMap<String, Any?>) : JsonBase, MutableMap<String, Any?>
        by map {

    override fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, level: Int) {
        result.append("{")

        var comma = false
        for ((k, v) in map) {
            if (comma) {
                result.append(",")
            } else {
                comma = true
            }

            if (prettyPrint) {
                result.appendln()
                result.indent(level + 1)
            }

            result.append("\"").append(k).append("\":")
            if (prettyPrint) {
                result.append(" ")
            }

            renderValue(v, result, prettyPrint, level + 1)
        }

        if (prettyPrint && map.isNotEmpty()) {
            result.appendln()
            result.indent(level)
        }

        result.append("}")
    }
}

public fun <T> JsonArray(vararg items : T) : JsonArray<T> =
    JsonArray(ArrayList(Arrays.asList(*items)))

public fun <T> JsonArray(list : List<T> = emptyList()) : JsonArray<T> =
        JsonArray(list.toArrayList())

public data class JsonArray<T>(val value : MutableList<T>) : JsonBase, MutableList<T> by value {

    override fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, level: Int) {
        result.append("[")

        var comma = false
        value.forEach {
            if (comma) {
                result.append(",")
                if (prettyPrint) {
                    result.append(" ")
                }
            } else {
                comma = true
            }

            renderValue(it, result, prettyPrint, level)
        }
        result.append("]")
    }

}
