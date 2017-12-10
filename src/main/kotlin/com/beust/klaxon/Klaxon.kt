package com.beust.klaxon

import java.io.*
import java.nio.charset.Charset
import kotlin.reflect.KClass

class Klaxon {
    val parser = Parser()

    /**
     * Parse a Reader into a JsonObject.
     */
    fun parseJsonObject(reader: Reader)
            = parser.parse(reader) as JsonObject

    /**
     * Parse a Reader into a JsonArray.
     */
    fun parseJsonArray(reader: Reader)
            = parser.parse(reader) as JsonArray<*>

    /**
     * Parse a JSON string into an object.
     */
    inline fun <reified T> parse(json: String) : T?
        = maybeParse(parser.parse(StringReader(json)))

    /**
     * Parse a JSON file into an object.
     */
    inline fun <reified T> parse(file: File) : T?
        = maybeParse(parser.parse(FileReader(file)))

    /**
     * Parse an InputStream into an object.
     */
    inline fun <reified T> parse(inputStream: InputStream) : T?
        = maybeParse(parser.parse(toReader(inputStream)))

    /**
     * Parse a JsonObject into an object.
     */
    inline fun <reified T> parseFromJsonObject(map: JsonObject) : T?
        = jsonAdapter.fromJsonObject(map, T::class.java) as T?

    /**
     * Add a TypeAdapter.
     */
    fun typeAdapter(annotation: KClass<out Annotation>, adapter: KlaxonAdapter<*>): Klaxon {
        jsonAdapter.typeAdapter(annotation, adapter)
        return this
    }

    //
    // Private
    //

    val jsonAdapter = JsonAdapter()

    private fun toInputStream(file: File) = FileInputStream(file)
    inline fun toReader(inputStream: InputStream, charset: Charset = Charsets.UTF_8)
            = inputStream.reader(charset)
    private fun toReader(file: File, charset: Charset = Charsets.UTF_8)
            = toReader(toInputStream(file), charset)

    private fun privateParse(file: File) = parser.parse(toReader(file))

    inline fun <reified T> maybeParse(map: Any?) : T? =
            if (map is JsonObject) parseFromJsonObject(map) else null
}
