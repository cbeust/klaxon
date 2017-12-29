package com.beust.klaxon

import com.beust.klaxon.internal.firstNotNullResult
import java.io.*
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaMethod

class Klaxon {
    private val DEFAULT_CONVERTER = object : Converter {
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

    private val converters = arrayListOf<Converter>(DEFAULT_CONVERTER)
    fun converter(converter: Converter): Klaxon {
        converters.add(0, converter)
        return this
    }

    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, Converter>()
    fun fieldConverter(annotation: KClass<out Annotation>, converter: Converter): Klaxon {
        fieldTypeMap[annotation] = converter
        return this
    }

    fun kFromJson(o: Any, prop: KProperty1<out Any, Any?>? = null): Any? {
        val converter = findFromConverter(o, prop)
        if (converter != null) {
            return converter.second
        } else {
            throw KlaxonException("Couldn't find a converter for $o")
        }
    }

    fun toJsonString(o: Any): String {
        val converted = findToConverter(o)
        if (converted != null) {
            return converted.second
        } else {
            throw KlaxonException("Couldn't find a converter for $o")
        }
    }

    fun findFromConverter(o: Any, prop: KProperty<*>?): Pair<Converter, Any>? {
        fun annotationsForProp(prop: KProperty<*>, kc: Class<*>): Array<out Annotation> {
            val result = kc.getDeclaredField(prop.name)?.declaredAnnotations ?: arrayOf()
            return result
        }

        fun annotationForProp(prop: KProperty<*>, kc: Class<*>, annotation: KClass<out Annotation>)
                = kc.getDeclaredField(prop.name).getDeclaredAnnotation(annotation.java)

        fun findFieldConverter() : Pair<Converter, Any>? {
            val result =
                if (prop != null) {
                    val cls = prop.getter.javaMethod!!.declaringClass
                    val allAnnotations = annotationsForProp(prop, cls)
                    val converter =
                            annotationsForProp(prop, cls).mapNotNull {
                                val annotation = annotationForProp(prop, cls, it.annotationClass)
                                fieldTypeMap[it.annotationClass]
                            }.firstOrNull()
                    if (converter != null) {
                        val js = converter.fromJson(JsonValue(o, this, null))
                        if (js != null) Pair(converter, js) else null
                    } else {
                        null
                    }
                } else {
                    null
                }
                return result
        }

        val fieldConverter = findFieldConverter()
        val result = fieldConverter ?:
            converters.firstNotNullResult {
                val js = it.fromJson(JsonValue(o, this, null))
                if (js != null) Pair(it, js) else null
            }

        return result
    }

    private fun findToConverter(o: Any): Pair<Converter, String>? {
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
                try {
                    prop.setter.call(obj, value)
                } catch(ex: IllegalArgumentException) {
                    throw KlaxonException("Can't set value $value on property $prop")
                }
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
                    val convertedValue = kFromJson(jValue, prop)
                    if (convertedValue != null) {
                        setField(this, prop, convertedValue)
                    } else {
                        val convertedValue = kFromJson(jValue, prop)
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
//        val js = Klaxon()
//                .converter(CARD_CONVERTER)
//                .toJsonString(deck)
//        println(js)
//    }
//}
