package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import com.beust.klaxon.internal.firstNotNullResult
import java.io.*
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

class Klaxon : ConverterFinder {
    /**
     * Parse a Reader into a JsonObject.
     */
    @Suppress("unused")
    fun parseJsonObject(reader: JsonReader)
            = parser.parse(reader) as JsonObject

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
            = maybeParse(parser.parse(StringReader(json)) as JsonObject)

    /**
     * Parse a JSON string into a JsonArray.
     */
    inline fun <reified T> parseArray(json: String): List<T>?
            = parseFromJsonArray(parser.parse(StringReader(json)) as JsonArray<*>)

    /**
     * Parse a JSON file into an object.
     */
    inline fun <reified T> parse(file: File): T?
            = maybeParse(parser.parse(FileReader(file)) as JsonObject)

    /**
     * Parse an InputStream into an object.
     */
    inline fun <reified T> parse(inputStream: InputStream): T? {
        return maybeParse(parser.parse(toReader(inputStream)) as JsonObject)
    }

    /**
     * Parse a JsonReader into an array.
     */
    inline fun <reified T> parse(jsonReader: JsonReader): T? {
        val p = Parser(jsonReader.lexer, streaming = true)
        return maybeParse(p.parse(jsonReader) as JsonObject)
    }

    /**
     * Parse a Reader into an object.
     */
    inline fun <reified T> parse(reader: Reader): T? {
        return maybeParse(parser.parse(reader) as JsonObject)
    }

    /**
     * Parse an InputStream into a JsonArray.
     */
    inline fun <reified T> parseArray(inputStream: InputStream): List<T>? {
        return parseFromJsonArray(parser.parse(toReader(inputStream)) as JsonArray<*>)
    }

    /**
     * Parse a JsonObject into an object.
     */
    inline fun <reified T> parseFromJsonObject(map: JsonObject): T?
            = fromJsonObject(map, T::class.java, T::class) as T?

    inline fun <reified T> parseFromJsonArray(map: JsonArray<*>): List<T>? {
        val result = arrayListOf<T>()
        map.forEach { jo ->
            if (jo is JsonObject) {
                val t = parseFromJsonObject<T>(jo)
                if (t != null) result.add(t)
                else throw KlaxonException("Couldn't convert $jo")
            } else {
                throw KlaxonException("Couldn't convert $jo")
            }
        }
        return result
    }

    inline fun <reified T> maybeParse(map: JsonObject): T? = parseFromJsonObject(map)

    fun toReader(inputStream: InputStream, charset: Charset = Charsets.UTF_8)
            = inputStream.reader(charset)

    val parser = Parser()

    private val DEFAULT_CONVERTER = DefaultConverter(this)

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

    fun findConverterFromClass(jc: Class<*>, prop: KProperty<*>?) : Converter<*> {
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

    /**
     * Convert a JsonObject into a real value.
     */
    fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>, kc: KClass<*>): Any {
        /**
         * Retrieve all the properties found on the class of the object and then look up each of these
         * properties names on `jsonObject`.
         */
        fun retrieveKeyValues() : Map<String, Any> {
            val result = hashMapOf<String, Any>()

            // Only keep the properties that are public and do not have @Json(ignored = true)
            val allProperties = Annotations.findNonIgnoredProperties(kc)

            allProperties?.forEach { prop ->
                //
                // Check if the name of the field was overridden with a @Json annotation
                //
                val prop = kc.declaredMemberProperties.first { it.name == prop.name }
                val jsonAnnotation = Annotations.findJsonAnnotation(kc, prop.name)
                val fieldName =
                        if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                        else prop.name

                // Retrieve the value of that property and convert it from JSON
                val jValue = jsonObject[fieldName]

                if (jValue != null) {
                    val convertedValue = findConverterFromClass(cls, prop)
                            .fromJson(JsonValue(jValue, prop, this@Klaxon))
                    if (convertedValue != null) {
                        result[prop.name] = convertedValue
                    } else {
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${prop::class} for "
                                + "field named \"${prop.name}\"")
                    }
                } else {
                    // Didn't find any value for that property: don't do anything. If a value is missing here,
                    // it might still be found as a default value on the constructor, and we'll find out once we
                    // try to instantiate that object.
                }
            }
            return result
        }

        /**
         * Go through all the constructors found on that object and attempt to invoke them with the key values
         * found on the object. We return the first successful instantiation, or fail with an exception if
         * no suitable constructor was found.
         */
        fun instantiateAndInitializeObject() : Any {
            val map = retrieveKeyValues()

            // Go through all the Kotlin constructors and associate each parameter with its value.
            // (Kotlin constructors contain the names of their parameters as opposed to Java constructors).
            // Note that this code will work for default parameters as well: values missing in the JSON map
            // will be filled by Kotlin reflection if they can't be found.
            var error: String? = null
            val result = kc?.constructors?.firstNotNullResult { constructor ->
                val parameterMap = hashMapOf<KParameter,Any>()
                constructor.parameters.forEach { parameter ->
                    map[parameter.name]?.let { convertedValue ->
                        parameterMap[parameter] = convertedValue
                    }
                }
                try {
                    if (! constructor.isAccessible) {
                        constructor.isAccessible = true
                    }
                    constructor.callBy(parameterMap)
                } catch(ex: Exception) {
                    // Lazy way to find out of that constructor worked. Easier than trying to make sure each
                    // parameter matches the parameter type.
                    error = ex::class.qualifiedName + " " + ex.message
                    null
                }
            }

            return result ?: throw KlaxonException(
                    "Couldn't find a suitable constructor for class ${kc?.simpleName} to initialize with $map: $error")
        }

        // If the user provided a type converter, use it, otherwise try to instantiate the object ourselves.
        val classConverter = findConverterFromClass(cls, null)
        val result =
            if (classConverter != DEFAULT_CONVERTER) {
                classConverter.fromJson(JsonValue(jsonObject, null, this@Klaxon)) as Any
            } else {
                instantiateAndInitializeObject()
            }
        return result
    }

    private fun log(s: String) {
        if (Debug.verbose) println(s)
    }
}
