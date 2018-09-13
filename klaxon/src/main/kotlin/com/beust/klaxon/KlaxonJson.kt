package com.beust.klaxon

import java.lang.IllegalArgumentException
import java.math.BigInteger

/**
 * The class used to define the DSL that generates JSON documents. All the functions defined in this class
 * can be used inside a `json { ... }` call.
 */
class KlaxonJson() {
    fun array(vararg args: Any?) : JsonArray<Any?> = JsonArray(args.map({ convert(it) }))

    fun array(args: List<Any?>) : JsonArray<Any?> = JsonArray(args.map({ convert(it) }))

    // we need this as now JsonArray<T> is List<T>
    fun <T> array(subArray : JsonArray<T>) : JsonArray<JsonArray<T>> = JsonArray(listOf(subArray))

    fun obj(vararg args: Pair<String, *>): JsonObject =
            JsonObject(linkedMapOf(*args).mapValues {convert(it.value)})

    companion object {
        private fun convert(value: Any?): Any? = when (value) {
            is Int -> value
            is Long -> value
            is String -> value
            is Boolean -> value
            is Float -> value.toDouble()
            is Double -> value
            is BigInteger -> value
            is JsonObject -> value
            is JsonArray<*> -> value
            null -> null
            else -> throw IllegalArgumentException("Unrecognized type: " + value)
        }
    }

}

/**
 * Main entry point.
 */
fun <T> json(init : KlaxonJson.() -> T) : T {
    return KlaxonJson().init()
}
