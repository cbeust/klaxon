package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import java.io.*
import java.lang.reflect.ParameterizedType
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

class Klaxon : ConverterFinder {
    /**
     * Parse a Reader into a JsonObject.
     */
    @Suppress("unused")
    fun parseJsonObject(reader: Reader)
            = parser.parse(reader) as JsonObject

    /**
     * Parse a Reader into a JsonArray.
     */
    @Suppress("unused")
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

    val parser = Parser()

    private val DEFAULT_CONVERTER = object : Converter<Any> {
        override fun fromJson(jv: JsonValue): Any {
            val value = jv.inside
            val result =
                when(value) {
                    is String -> value
                    is Boolean -> value
                    is Int -> value
                    is Collection<*> -> value.map {
                        val jt = jv.property.returnType.javaType
                        // Try to find a converter for the element type of the collection
                        val converter =
                            if (jt is ParameterizedType) {
                                val cls = jt.actualTypeArguments[0] as Class<*>
                                findConverterFromClass(cls, null)
                            } else {
                                if (it != null) {
                                    findConverter(it)
                                } else {
                                    throw KlaxonException("Don't know how to convert null value in array $jv")
                                }
                            }

                        converter.fromJson(JsonValue(it, jv.property, this@Klaxon))
                    }
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
                        prop.getter.call(value)?.let { getValue ->
                            val jsonValue = toJsonString(getValue)
                            valueList.add("\"${prop.name}\" : $jsonValue")
                        }
                    }
                    return "{" + valueList.joinToString(", ") + "}"
                }

            }
            return result
        }
    }

    /**
     * Type converters that convert a JsonObject into an object.
     */
    private val converters = arrayListOf<Converter<*>>(DEFAULT_CONVERTER)
    private val converterMap = hashMapOf<java.lang.reflect.Type, Converter<*>>()

    fun converter(converter: Converter<*>): Klaxon {

//        fun extractAnnotation(ann: KClass<out Annotation>, function: KFunction<*>): KType? {
//            val from = function.annotations.filter { it.annotationClass == ann }
//            val result =
//                if (from.any()) {
//                    function.parameters[1].type
//                } else {
//                    null
//                }
//            return result
//        }

        var cls: java.lang.reflect.Type? = null
        converter::class.declaredFunctions.forEach { f ->
            if (f.name == "toJson") {
                cls = f.parameters.firstOrNull { it.kind == KParameter.Kind.VALUE }?.type?.javaType
            }
//            extractAnnotation(FromJson::class, f)?.let { fromType ->
//                fromMap[fromType.javaType] = f
//            }
//            extractAnnotation(ToJson::class, f)?.let { toType ->
//                toMap[toType.javaType] = f
//            }
        }
        converters.add(0, converter)
        if (cls != null) {
            converterMap[cls!!] = converter
        } else {
            throw KlaxonException("Couldn't identify which type this converter converts: $converter")
        }
        return this
    }

    /**
     * Field type converters that convert fields with a marker annotation.
     */
    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, Converter<*>>()

    fun fieldConverter(annotation: KClass<out Annotation>, converter: Converter<*>): Klaxon {
        fieldTypeMap[annotation] = converter
        return this
    }

    /**
     * @return a converter that will turn `value` into a `JsonObject`. If a non-null property is
     * passed, inspect that property for annotations that would override the type converter
     * we need to use to convert it.
     */
    override fun findConverter(value: Any, prop: KProperty<*>?): Converter<*> {
        val result =
//            if (value is Collection<*> && prop != null) {
//                val cls = (prop.returnType.javaType as ParameterizedTypeImpl).actualTypeArguments[0] as Class<*>
//                findConverterFromClass(cls, null)
//            } else {
                findConverterFromClass(value::class.java, prop)
//            }
        log("Value: $value, converter: $result")
        return result
    }

    private fun findConverterFromClass(jc: Class<*>, prop: KProperty<*>?) : Converter<*> {
        fun annotationsForProp(prop: KProperty<*>, kc: Class<*>): Array<out Annotation> {
            val result = kc.getDeclaredField(prop.name)?.declaredAnnotations ?: arrayOf()
            return result
        }

        var cls: Class<*>? = null
        val propConverter =
            if (prop != null) {
                cls = prop.getter.javaMethod!!.returnType
                val dc = prop.getter.javaMethod!!.declaringClass
                annotationsForProp(prop, dc).mapNotNull {
                    fieldTypeMap[it.annotationClass]
                }.firstOrNull()
            } else {
                null
            }

        val result = propConverter ?: converterMap[cls ?: jc] ?: DEFAULT_CONVERTER
        return result
    }

    fun toJsonString(value: Any): String {
        val converter = findConverter(value)
        // It's not possible to safely call converter.toJson(value) since its parameter is generic,
        // so use reflection
        val toJsonMethod = converter::class.functions.firstOrNull { it.name == "toJson" }
        val result =
            if (toJsonMethod != null) {
                toJsonMethod.call(converter, value) as String
            } else {
                throw KlaxonException("Couldn't find a toJson() function on converter $converter")
            }
        return result
    }

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
                //
                // Check if the name of the field was overridden with a @Json annotation
                //
                val jsonAnnotation = kc.java.getDeclaredField(prop.name).getDeclaredAnnotation(Json::class.java)
                val fieldName =
                        if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                        else prop.name

                // Retrieve the value of that property and convert it from JSON
                val jValue = jsonObject[fieldName]

                if (jValue == null) {
                    val jsonFields = jsonObject.keys.joinToString(",")
                    throw KlaxonException("Don't know how to map class field \"$fieldName\" " +
                            "to any JSON field: $jsonFields")
                } else {
                    val convertedValue = findConverter(jValue, prop).fromJson(JsonValue(jValue, prop, this@Klaxon))
                    if (convertedValue != null) {
                        setField(this, prop, convertedValue)
                    } else {
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${prop::class} for "
                                + "field named \"${prop.name}\"")
                    }
                }

            }
        }
        return result
    }

    private fun log(s: String) {
//        println(s)
    }
}
