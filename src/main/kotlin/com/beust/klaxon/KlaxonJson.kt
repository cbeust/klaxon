package com.beust.klaxon

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
            is Float -> value.toDouble()
            null -> null
            else -> value
        }
    }

}

/**
 * Main entry point.
 */
fun <T> json(init : KlaxonJson.() -> T) : T {
    return KlaxonJson().init()
}
