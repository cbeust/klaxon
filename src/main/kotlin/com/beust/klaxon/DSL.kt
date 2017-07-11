package com.beust.klaxon

import java.util.*

fun valueToString(v: Any?, prettyPrint: Boolean = false, canonical : Boolean = false) : String =
    StringBuilder().apply {
        renderValue(v, this, prettyPrint, canonical, 0)
    }.toString()

interface JsonBase {
    fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, canonical: Boolean, level: Int)
    fun appendJsonString(result : Appendable, prettyPrint: Boolean = false, canonical: Boolean = false) =
            appendJsonStringImpl(result, prettyPrint, canonical, 0)
    fun toJsonString(prettyPrint: Boolean = false, canonical: Boolean = false) : String =
            StringBuilder().apply { appendJsonString(this, prettyPrint, canonical) }.toString()
}

fun JsonObject(map : Map<String, Any?> = emptyMap()) : JsonObject =
        JsonObject(LinkedHashMap(map))

data class JsonObject(val map: MutableMap<String, Any?>) : JsonBase, MutableMap<String, Any?>
        by map {

    override fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, canonical: Boolean, level: Int) {
        result.append("{")

        var comma = false
        for ((k, v) in (if(canonical) map.toSortedMap() else map)) {
            if (comma) {
                result.append(",")
            } else {
                comma = true
            }

            if (prettyPrint && !canonical) {
                result.appendln()
                result.indent(level + 1)
            }

            result.append("\"").append(k).append("\":")
            if (prettyPrint && !canonical) {
                result.append(" ")
            }

            renderValue(v, result, prettyPrint, canonical, level + 1)
        }

        if (prettyPrint && !canonical && map.isNotEmpty()) {
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

    override fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, canonical : Boolean, level: Int) {
        result.append("[")

        var comma = false
        value.forEach {
            if (comma) {
                result.append(",")
                if (prettyPrint && !canonical) {
                    result.append(" ")
                }
            } else {
                comma = true
            }

            renderValue(it, result, prettyPrint, canonical, level)
        }
        result.append("]")
    }

}
