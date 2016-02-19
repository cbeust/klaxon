package com.beust.klaxon

import java.util.*

fun valueToString(v: Any?, prettyPrint: Boolean = false) : String =
    StringBuilder().apply {
        renderValue(v, this, prettyPrint, 0)
    }.toString()

interface JsonBase {
    fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, level: Int)
    fun appendJsonString(result : Appendable, prettyPrint: Boolean = false) =
            appendJsonStringImpl(result, prettyPrint, 0)
    fun toJsonString(prettyPrint: Boolean = false) : String =
            StringBuilder().apply { appendJsonString(this, prettyPrint) }.toString()
}

fun JsonObject(map : Map<String, Any?> = emptyMap()) : JsonObject =
        JsonObject(LinkedHashMap(map))

data class JsonObject(val map: MutableMap<String, Any?>) : JsonBase, MutableMap<String, Any?>
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

fun <T> JsonArray(vararg items : T) : JsonArray<T> =
    JsonArray(ArrayList(Arrays.asList(*items)))

fun <T> JsonArray(list : List<T> = emptyList()) : JsonArray<T> =
        JsonArray(list.toMutableList())

data class JsonArray<T>(val value : MutableList<T>) : JsonBase, MutableList<T> by value {

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
