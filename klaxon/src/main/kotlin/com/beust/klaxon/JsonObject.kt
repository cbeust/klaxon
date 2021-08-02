package com.beust.klaxon

import java.math.BigInteger
import java.util.*

fun JsonObject(map: Map<Any, Any?> = emptyMap()): JsonObject =
    JsonObject(LinkedHashMap(map))

data class JsonObject(val map: MutableMap<Any, Any?>) : JsonBase, MutableMap<Any, Any?>
by map {
//    constructor() : this(mutableMapOf<Any, Any?>()) {}

    override fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, canonical: Boolean, level: Int) {
        fun indent(a: Appendable, level: Int) {
            for (i in 1..level) {
                a.append("  ")
            }
        }


        result.append('{')
        var comma = false
        for ((k, v) in (if (canonical) map.toSortedMap { a, b -> a.toString().compareTo(b?.toString() ?: "") } else map)) {
            if (comma) {
                result.append(',')
            } else {
                comma = true
            }

            if (prettyPrint && !canonical) {
                result.appendLine()
                indent(result, level + 1)
            }

            result.append(Render.renderString(k.toString())).append(':')
            if (prettyPrint && !canonical) {
                result.append(" ")
            }

            Render.renderValue(v, result, prettyPrint, canonical, level + 1)
        }

        if (prettyPrint && !canonical && map.isNotEmpty()) {
            result.appendLine()
            indent(result, level)
        }

        result.append('}')
    }

    override fun toString() = keys.joinToString(",")

    @Suppress("UNCHECKED_CAST")
    fun <T> array(fieldName: String): JsonArray<T>? = get(fieldName) as JsonArray<T>?

    fun obj(fieldName: String): JsonObject? = get(fieldName) as JsonObject?

    fun int(fieldName: String): Int? {
        val value = get(fieldName)
        return when (value) {
            is Number -> value.toInt()
            else -> value as Int?
        }
    }

    fun long(fieldName: String): Long? {
        val value = get(fieldName)
        return when (value) {
            is Number -> value.toLong()
            else -> value as Long?
        }
    }

    fun bigInt(fieldName: String): BigInteger? = get(fieldName) as BigInteger
    fun string(fieldName: String): String? = get(fieldName) as String?
    fun double(fieldName: String): Double? = get(fieldName) as Double?
    fun float(fieldName: String): Float? = (get(fieldName) as Double?)?.toFloat()
    fun boolean(fieldName: String): Boolean? = get(fieldName) as Boolean?


}
