package com.beust.klaxon


@Suppress("UNCHECKED_CAST")
private fun <T> Any?.ensureArray() : JsonArray<T> =
        if (this is JsonArray<*>) this as JsonArray<T>
        else JsonArray(this as T)

fun <T> JsonBase.lookup(key : String) : JsonArray<T> =
        key.split("[/\\.]".toRegex())
                .filter{ it != "" }
                .fold(this) { j, part ->
                    when (j) {
                        is JsonArray<*> -> j[part]
                        is JsonObject -> j[part].ensureArray<T>()
                        else -> throw IllegalArgumentException("unsupported type of j = $j")
                    }
                }.ensureArray<T>()

