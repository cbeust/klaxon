package com.beust.klaxon

import com.beust.klaxon.Render.renderValue
import java.util.*

fun valueToString(v: Any?, prettyPrint: Boolean = false, canonical : Boolean = false) : String =
    StringBuilder().apply {
        Render.renderValue(v, this, prettyPrint, canonical, 0)
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

fun Appendable.indent(level: Int) {
    for (i in 1..level) {
        append("  ")
    }
}

data class JsonObject(val map: MutableMap<String, Any?>) : JsonBase, MutableMap<String, Any?>
        by map {
//    constructor() : this(mutableMapOf<String, Any?>()) {}

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

// Needs to be a separate function since as a constructor, its signature conflicts with the List constructor
fun <T> JsonArray(list : List<T> = emptyList()) : JsonArray<T> =
        JsonArray(list.toMutableList())

data class JsonArray<T>(val value : MutableList<T>) : JsonBase, MutableList<T> by value {
    constructor(vararg items : T) : this(ArrayList(Arrays.asList(*items)))

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
