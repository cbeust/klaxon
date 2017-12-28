package com.beust.klaxon

import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.*
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

interface Converter3 {
    fun toJson(o: Any): String?
    fun fromJson(jv: JsonValue) : Any?
}

class Klaxon3 {
    private val DEFAULT_CONVERTER = object : Converter3 {
        override fun fromJson(jv: JsonValue): Any? {
            val value = jv.inside
            val result =
                when(value) {
                    is String -> value
                    is Boolean -> value
                    is Int -> value
                    is Collection<*> -> value.map { if (it != null) kFromJson(it) else null }
                    else -> throw KlaxonException("Don't know how to convert $value")
                }
            return result
        }

        override fun toJson(value: Any): String? {
            val result = when (value) {
                is Boolean -> value.toString()
                is String -> "\"" + value + "\""
                is Int -> value.toString()
                is Collection<*> -> {
                    val elements = value.filterNotNull().map { toJson(it) }
                    "[" + elements.joinToString(", ") + "]"
                }
                else -> {
                    val valueList = arrayListOf<String>()
                    value::class.declaredMemberProperties.forEach { prop ->
                        val value = prop.getter.call(value)
                        if (value != null) {
                            val jsonValue = toJsonString(value)
                            valueList.add("\"${prop.name}\" : $jsonValue")
                        }
                    }
                    return "{" + valueList.joinToString(", ") + "}"
                }

            }
            return result
        }
    }

    private val converters = arrayListOf<Converter3>(DEFAULT_CONVERTER)

    fun converter(converter: Converter3): Klaxon3 {
        converters.add(0, converter)
        return this
    }

    fun kFromJson(o: Any): Any? {
        val converter = findFromConverter(o)
        if (converter != null) {
            return converter.second
        } else {
            throw KlaxonException("Couldn't find a converter for $o")
        }
    }

    fun toJsonString(o: Any): String {
        val converter = findToConverter(o)
        if (converter != null) {
            return converter.second
        } else {
            throw KlaxonException("Couldn't find a converter for $o")
        }
    }

    fun findFromConverter(o: Any): Pair<Converter3, Any>? {
        val result = converters.firstNotNullResult {
            val js = it.fromJson(JsonValue(o, this, null))
            if (js != null) Pair(it, js) else null
        }
        return result
    }

    private fun findToConverter(o: Any): Pair<Converter3, String>? {
        val result = converters.mapNotNull {
            val js = it.toJson(o)
            if (js != null) Pair(it, js) else null
        }
        return result.firstOrNull()
    }

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
    inline fun <reified T> parse(json: String): T?
            = maybeParse(parser.parse(StringReader(json)))

    /**
     * Parse a JSON file into an object.
     */
    inline fun <reified T> parse(file: File): T?
            = maybeParse(parser.parse(FileReader(file)))

    /**
     * Parse an InputStream into an object.
     */
    inline fun <reified T> parse(inputStream: InputStream): T?
            = maybeParse(parser.parse(toReader(inputStream)))

    /**
     * Parse a JsonObject into an object.
     */
    inline fun <reified T> parseFromJsonObject(map: JsonObject): T?
            = fromJsonObject(map, T::class.java, T::class) as T?

    inline fun <reified T> maybeParse(map: Any?): T? =
            if (map is JsonObject) parseFromJsonObject(map) else null

    fun toReader(inputStream: InputStream, charset: Charset = Charsets.UTF_8)
            = inputStream.reader(charset)

    fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>, kc: KClass<*>?): Any {
        fun setField(obj: Any, prop: KProperty<*>, value: Any) {
            if (prop is KMutableProperty<*>) {
                prop.setter.call(obj, value)
            } else {
                throw KlaxonException("Property $prop is not mutable")
            }
        }

        val result = cls.newInstance().apply {
            kc?.declaredMemberProperties?.forEach { prop ->
                val jsonAnnotation = kc.java.getDeclaredField(prop.name).getDeclaredAnnotation(Json::class.java)
                val fieldName =
                        if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                        else prop.name
                val jValue = jsonObject[fieldName]

                println("Prop: $prop")

                if (jValue == null) {
                    val jsonFields = jsonObject.keys.joinToString(",")
                    throw KlaxonException("Don't know how to map class field \"$fieldName\" " +
                            "to any JSON field: $jsonFields")
                } else {
                    val convertedValue = kFromJson(jValue)
                    if (convertedValue != null) {
                        setField(this, prop, convertedValue)
                    } else {
                        val convertedValue = kFromJson(jValue)
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${prop::class} for "
                                + "field named \"${prop.name}\"")
                    }
                }

            }
        }
        return result
    }

    /**
     * Gather a list of all candidate type converters and take the first one that will convert
     * the value. @return null otherwise.
     */
//    fun tryToConvert(prop: KProperty1<out Any, Any?>, value: JsonValue): Any? {
//        val candidates = converters.map { it.fromJson(value) }
//        return null
//    }
}

//fun main(args: Array<String>) {
//
//    run {
//
//    }
//    run {
//        val deck = Deck2(cardCount = 2, cards = listOf(Card(8, "Hearts"), Card(3, "Spades")))
//        val js = Klaxon3()
//                .converter(CARD_CONVERTER)
//                .toJsonString(deck)
//        println(js)
//    }
//}
