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

    fun array(vararg args: Any) : JsonArray<Any?> = JsonArray(args.map(::convert))

    fun array(args: List<Any>) : JsonArray<Any?> = JsonArray(args.map(::convert))

    fun obj(vararg args: Pair<String, *>): JsonObject = JsonObject(linkedMapOf(*args).mapValues {convert(it.getValue())})
}

public fun <T> json(init : JSON.() -> T) : T {
    return JSON().init()
}
