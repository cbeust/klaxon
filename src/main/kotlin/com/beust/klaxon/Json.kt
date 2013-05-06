package com.beust.klaxon

import java.util.HashMap

class JSON() {

    private fun convert(value: Any) : Any {
        val result: Any
        when (value) {
            is Int -> result = value
            is Long -> result = value
            is String -> result = value
            is Boolean -> result = value
            is Float -> result = value
            is Double -> result = value
            is JsonObject -> result = value
            is JsonArray<*> -> result = value
            else -> throw IllegalArgumentException("Unrecognized type: " + value)
        }
        return result
    }

    fun array(vararg args: Any) : JsonArray<Any> {
        val result = JsonArray<Any>()
        var i = 0
        while (i < args.size) {
            result.add(convert(args[i++]))
        }
        return result
    }

    fun obj(vararg args: Any): JsonObject {
        val result = JsonObject()
        var i = 0
        while (i < args.size) {
            val k = args[i]
            if (k !is String) {
                throw IllegalArgumentException("Key should be a string: ${k}")
            }
            val key = k as String
            val value = args[i + 1]
            result.put(key, convert(value))
            i += 2
        }
        return result
    }
}

fun <T> json(init : JSON.() -> T) : T {
    return JSON().init()
}
