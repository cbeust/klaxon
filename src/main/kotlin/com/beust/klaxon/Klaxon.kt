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
        = jsonConverter.fromJsonObject(map, T::class.java, T::class) as T?

    /**
     * Add a TypeConverter.
     */
    fun typeConverter(adapter: Converter2): Klaxon {
        jsonConverter.typeConverter(adapter)
        return this
    }

    /**
     * Add a field TypeConverter.
     */
    fun fieldConverter(annotation: KClass<out Annotation>, adapter: Converter2): Klaxon {
        jsonConverter.fieldTypeConverter(annotation, adapter)
        return this
    }

    //
    // Private
    //

    val jsonConverter = JsonConverter()

    fun toReader(inputStream: InputStream, charset: Charset = Charsets.UTF_8)
            = inputStream.reader(charset)

    inline fun <reified T> maybeParse(map: Any?) : T? =
            if (map is JsonObject) parseFromJsonObject(map) else null

    fun toJsonString(obj: Any): String {
        val converter = jsonConverter.findBestConverter(obj)
        if (converter != null) {
            return converter.toJson(obj)
        } else {
            return "<couldn't find a converter>"
        }
    }
}
