package com.beust.klaxon

import java.math.BigInteger
import java.util.ArrayList


@Suppress("UNCHECKED_CAST")
fun <T> JsonObject.array(fieldName: String) : JsonArray<T>? = get(fieldName) as JsonArray<T>?
fun JsonObject.obj(fieldName: String) : JsonObject? = get(fieldName) as JsonObject?
fun JsonObject.int(fieldName: String) : Int? {
    val value = get(fieldName)
    when (value) {
        is Number -> return value.toInt()
        else -> return value as Int?
    }
}
fun JsonObject.long(fieldName: String) : Long? {
    val value = get(fieldName)
    when (value) {
        is Number -> return value.toLong()
        else -> return value as Long?
    }
}
fun JsonObject.bigInt(fieldName: String) : BigInteger? = get(fieldName) as BigInteger
fun JsonObject.string(fieldName: String) : String? = get(fieldName) as String?
fun JsonObject.double(fieldName: String) : Double? = get(fieldName) as Double?
fun JsonObject.boolean(fieldName: String) : Boolean? = get(fieldName) as Boolean?


fun JsonArray<*>.string(id: String) : JsonArray<String?> = mapChildren { it.string(id) }
fun JsonArray<*>.obj(id: String) : JsonArray<JsonObject?> = mapChildren { it.obj(id) }
fun JsonArray<*>.long(id: String) : JsonArray<Long?> = mapChildren { it.long(id) }
fun JsonArray<*>.int(id: String) : JsonArray<Int?> = mapChildren { it.int(id) }
fun JsonArray<*>.bigInt(id: String) : JsonArray<BigInteger?> = mapChildren { it.bigInt(id) }
fun JsonArray<*>.double(id: String) : JsonArray<Double?> = mapChildren { it.double(id) }

fun <T> JsonArray<*>.mapChildrenObjectsOnly(block : (JsonObject) -> T) : JsonArray<T> =
        JsonArray(flatMapTo(ArrayList<T>(size)) {
            if (it is JsonObject) listOf(block(it))
            else if (it is JsonArray<*>) it.mapChildrenObjectsOnly(block)
            else listOf()
        })

fun <T : Any> JsonArray<*>.mapChildren(block : (JsonObject) -> T?) : JsonArray<T?> =
        JsonArray(flatMapTo(ArrayList<T?>(size)) {
            if (it is JsonObject) listOf(block(it))
            else if (it is JsonArray<*>) it.mapChildren(block)
            else listOf(null)
        })

operator fun JsonArray<*>.get(key : String) : JsonArray<Any?> = mapChildren { it[key] }

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

