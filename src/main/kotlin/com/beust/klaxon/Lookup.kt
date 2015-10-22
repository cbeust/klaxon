package com.beust.klaxon

import java.math.BigInteger
import java.util.ArrayList


@Suppress("UNCHECKED_CAST")
public fun <T> JsonObject.array(fieldName: String) : JsonArray<T>? = get(fieldName) as JsonArray<T>?
public fun JsonObject.obj(fieldName: String) : JsonObject? = get(fieldName) as JsonObject?
public fun JsonObject.int(fieldName: String) : Int? {
    val value = get(fieldName)
    when (value) {
        is Number -> return value.toInt()
        else -> return value as Int?
    }
}
public fun JsonObject.long(fieldName: String) : Long? {
    val value = get(fieldName)
    when (value) {
        is Number -> return value.toLong()
        else -> return value as Long?
    }
}
public fun JsonObject.bigInt(fieldName: String) : BigInteger? = get(fieldName) as BigInteger
public fun JsonObject.string(fieldName: String) : String? = get(fieldName) as String?
public fun JsonObject.double(fieldName: String) : Double? = get(fieldName) as Double?
public fun JsonObject.boolean(fieldName: String) : Boolean? = get(fieldName) as Boolean?


public fun JsonArray<*>.string(id: String) : JsonArray<String?> = mapChildren { it.string(id) }
public fun JsonArray<*>.obj(id: String) : JsonArray<JsonObject?> = mapChildren { it.obj(id) }
public fun JsonArray<*>.long(id: String) : JsonArray<Long?> = mapChildren { it.long(id) }
public fun JsonArray<*>.int(id: String) : JsonArray<Int?> = mapChildren { it.int(id) }
public fun JsonArray<*>.bigInt(id: String) : JsonArray<BigInteger?> = mapChildren { it.bigInt(id) }
public fun JsonArray<*>.double(id: String) : JsonArray<Double?> = mapChildren { it.double(id) }

public fun <T> JsonArray<*>.mapChildrenObjectsOnly(block : (JsonObject) -> T) : JsonArray<T> =
        JsonArray(flatMapTo(ArrayList(size)) {
            if (it is JsonObject) listOf(block(it))
            else if (it is JsonArray<*>) it.mapChildrenObjectsOnly(block)
            else listOf()
        })

public fun <T : Any> JsonArray<*>.mapChildren(block : (JsonObject) -> T?) : JsonArray<T?> =
        JsonArray(flatMapTo(ArrayList(size)) {
            if (it is JsonObject) listOf(block(it))
            else if (it is JsonArray<*>) it.mapChildren(block)
            else listOf(null)
        })

operator public fun JsonArray<*>.get(key : String) : JsonArray<Any?> = mapChildren { it[key] }

@Suppress("UNCHECKED_CAST")
private fun Any?.ensureArray() : JsonArray<Any?> =
        if (this is JsonArray<*>) this as JsonArray<Any?>
        else JsonArray(this)

public fun JsonBase.lookup(key : String) : JsonArray<Any?> =
    key.split("[/\\.]".toRegex())
            .filter{ it != "" }
            .fold(this) { j, part ->
                when (j) {
                    is JsonArray<*> -> j[part]
                    is JsonObject -> j[part].ensureArray()
                    else -> throw IllegalArgumentException("unsupported type of j = $j")
                }
            }.ensureArray()

