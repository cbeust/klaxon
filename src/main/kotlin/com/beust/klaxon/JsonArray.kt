package com.beust.klaxon

import java.math.BigInteger
import java.util.*

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

            Render.renderValue(it, result, prettyPrint, canonical, level)
        }
        result.append("]")
    }

    fun string(id: String) : JsonArray<String?> = mapChildren { it.string(id) }
    fun obj(id: String) : JsonArray<JsonObject?> = mapChildren { it.obj(id) }
    fun long(id: String) : JsonArray<Long?> = mapChildren { it.long(id) }
    fun int(id: String) : JsonArray<Int?> = mapChildren { it.int(id) }
    fun bigInt(id: String) : JsonArray<BigInteger?> = mapChildren { it.bigInt(id) }
    fun double(id: String) : JsonArray<Double?> = mapChildren { it.double(id) }

    fun <T> mapChildrenObjectsOnly(block : (JsonObject) -> T) : JsonArray<T> =
            JsonArray(flatMapTo(ArrayList<T>(size)) {
                if (it is JsonObject) listOf(block(it))
                else if (it is JsonArray<*>) it.mapChildrenObjectsOnly(block)
                else listOf()
            })

    fun <T : Any> mapChildren(block : (JsonObject) -> T?) : JsonArray<T?> =
            JsonArray(flatMapTo(ArrayList<T?>(size)) {
                if (it is JsonObject) listOf(block(it))
                else if (it is JsonArray<*>) it.mapChildren(block)
                else listOf(null)
            })

    operator fun get(key : String) : JsonArray<Any?> = mapChildren { it[key] }
}
