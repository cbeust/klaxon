package com.beust.klaxon

import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/**
 * The class used to define the DSL that generates JSON documents. All the functions defined in this class
 * can be used inside a `json { ... }` call.
 */
class KlaxonJson {
    fun array(vararg args: Any?) : JsonArray<Any?> = JsonArray(args.map { convert(it) })

    fun array(args: List<Any?>) : JsonArray<Any?> = JsonArray(args.map { convert(it) })

    // we need this as now JsonArray<T> is List<T>
    fun <T> array(subArray : JsonArray<T>) : JsonArray<JsonArray<T>> = JsonArray(listOf(subArray))

    fun obj(args: Iterable<Pair<String, *>>): JsonObject =
            JsonObject(args.toMap(LinkedHashMap()).mapValues { convert(it.value) })

    fun obj(vararg args: Pair<String, *>): JsonObject =
            obj(args.toList())

    fun obj(key: String, init: KlaxonJson.() -> Unit): JsonObject {
        stackMap.push(LinkedHashMap<String, Any?>())
        theKlaxonJson.init()
        val map = stackMap.pop()
        val newMap = if (stackMap.isEmpty()) HashMap() else stackMap.peek()
        newMap[key] = JsonObject(map.mapValues { convert(it.value) })
        return JsonObject(newMap)
    }

    private val stackMap = Stack<HashMap<String, Any?>>()

    fun put(key: String, value: Any?) {
        stackMap.peek()[key] = convert(value)
    }

    companion object {

        internal val theKlaxonJson = KlaxonJson()

        private fun convert(value: Any?): Any? = when (value) {
            is Float -> value.toDouble()
            is Short -> value.toInt()
            is Byte -> value.toInt()
            null -> null
            else -> value
        }
    }

}

/**
 * Main entry point.
 */
fun <T> json(init : KlaxonJson.() -> T) : T {
    return KlaxonJson.theKlaxonJson.init()
}
