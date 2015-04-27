package com.beust.klaxon

fun convert(value: Any?) : Any? = when (value) {
        is Int -> value
        is Long -> value
        is String -> value
        is Boolean -> value
        is Float -> value
        is Double -> value
        is JsonObject -> value
        is JsonArray<*> -> value
        null -> null
        else -> throw IllegalArgumentException("Unrecognized type: " + value)
    }

class JSON() {

    fun array(vararg args: Any?) : JsonArray<Any?> = JsonArray(args.map(::convert))

    fun array(args: List<Any?>) : JsonArray<Any?> = JsonArray(args.map(::convert))

    // we need this as now JsonArray<T> is List<T>
    fun <T> array(subArray : JsonArray<T>) : JsonArray<JsonArray<T>> = JsonArray(listOf(subArray))

    fun obj(vararg args: Pair<String, *>): JsonObject =
            JsonObject(linkedMapOf(*args).mapValues {convert(it.getValue())})
}

/**
 * Main entry point.
 */
public fun <T> json(init : JSON.() -> T) : T {
    return JSON().init()
}
