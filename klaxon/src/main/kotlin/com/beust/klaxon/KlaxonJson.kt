package com.beust.klaxon

/**
 * The class used to define the DSL that generates JSON documents. All the functions defined in this class
 * can be used inside a `json { ... }` call.
 */
interface KlaxonJson {
    fun array(vararg args: Any?) : JsonArray<Any?> = JsonArray(args.map { convert(it) })

    fun array(args: List<Any?>) : JsonArray<Any?> = JsonArray(args.map { convert(it) })

    // we need this as now JsonArray<T> is List<T>
    fun <T> array(subArray : JsonArray<T>) : JsonArray<JsonArray<T>> = JsonArray(listOf(subArray))

    fun obj(args: Iterable<Pair<String, *>>): JsonObject =
            JsonObject(args.toMap(LinkedHashMap()).mapValues { convert(it.value) })

    fun obj(vararg args: Pair<String, *>): JsonObject =
            obj(args.toList())

    companion object {

        internal val theKlaxonJson = object : KlaxonJson { }

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
