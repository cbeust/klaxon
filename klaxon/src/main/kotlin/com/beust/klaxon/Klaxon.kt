package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import java.io.*
import java.nio.charset.Charset
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

class Klaxon(
    val instanceSettings: KlaxonSettings = KlaxonSettings()
) : ConverterFinder {
    /**
     * Parse a JsonReader into a JsonObject.
     */
    @Suppress("unused")
    fun parseJsonObject(reader: JsonReader)
            = parser().parse(reader) as JsonObject

    /**
     * Parse a Reader into a JsonObject.
     */
    @Suppress("unused")
    fun parseJsonObject(reader: Reader)
            = parser().parse(reader) as JsonObject

    /**
     * Parse a Reader into a JsonArray.
     */
    @Suppress("unused")
    fun parseJsonArray(reader: Reader)
            = parser().parse(reader) as JsonArray<*>

    /**
     * Parse a JSON string into an object.
     */
    @Suppress("unused")
    inline fun <reified T> parse(json: String): T?
            = maybeParse(parser(T::class).parse(StringReader(json)) as JsonObject)

    /**
     * Parse a JSON string into a List.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(json: String): List<T>?
            = parseFromJsonArray(parser(T::class).parse(StringReader(json)) as JsonArray<*>)

    /**
     * Parse a JSON file into an object.
     */
    @Suppress("unused")
    inline fun <reified T> parse(file: File): T? = FileReader(file).use { reader ->
        maybeParse(parser(T::class).parse(reader) as JsonObject)
    }

    /**
     * Parse a JSON file into a List.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(file: File): List<T>? = FileReader(file).use { reader ->
        parseFromJsonArray(parser(T::class).parse(reader) as JsonArray<*>)
    }

    /**
     * Parse an InputStream into an object.
     */
    @Suppress("unused")
    inline fun <reified T> parse(inputStream: InputStream): T? {
        return maybeParse(parser(T::class).parse(toReader(inputStream)) as JsonObject)
    }

    /**
     * Parse an InputStream into a List.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(inputStream: InputStream): List<T>?
            = parseFromJsonArray(parser(T::class).parse(toReader(inputStream)) as JsonArray<*>)

    /**
     * Parse a JsonReader into an object.
     */
    @Suppress("unused")
    inline fun <reified T> parse(jsonReader: JsonReader): T? {
        val p = parser(T::class, jsonReader.lexer, streaming = true)
        return maybeParse(p.parse(jsonReader) as JsonObject)
    }

    /**
     * Parse a JsonReader into a List.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(jsonReader: JsonReader): List<T>? {
        val p = parser(T::class, jsonReader.lexer, streaming = true)
        return parseFromJsonArray(p.parse(jsonReader) as JsonArray<*>)
    }

    /**
     * Parse a Reader into an object.
     */
    @Suppress("unused")
    inline fun <reified T> parse(reader: Reader): T? {
        return maybeParse(parser(T::class).parse(reader) as JsonObject)
    }

    /**
     * Parse a Reader into a List.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(reader: Reader): List<T>? {
        return parseFromJsonArray(parser(T::class).parse(reader) as JsonArray<*>)
    }

    /**
     * Parse a JsonObject into an object.
     */
    inline fun <reified T> parseFromJsonObject(map: JsonObject): T?
        = fromJsonObject(map, T::class.java, T::class) as T?

    inline fun <reified T> parseFromJsonArray(map: JsonArray<*>): List<T>? {
        val result = arrayListOf<Any?>()
        map.forEach { jo ->
            if (jo is JsonObject) {
                val t = parseFromJsonObject<T>(jo)
                if (t != null) result.add(t)
                else throw KlaxonException("Couldn't convert $jo")
            } else if (jo != null) {
                val converter = findConverterFromClass(T::class.java, null)
                val convertedValue = converter.fromJson(JsonValue(jo, null, null, this))
                result.add(convertedValue)
            } else {
                throw KlaxonException("Couldn't convert $jo")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as List<T>
    }

    inline fun <reified T> maybeParse(map: JsonObject): T? = parseFromJsonObject(map)

    fun toReader(inputStream: InputStream, charset: Charset = Charsets.UTF_8)
            = inputStream.reader(charset)

    @Suppress("MemberVisibilityCanBePrivate")
    val pathMatchers = arrayListOf<PathMatcher>()

    fun pathMatcher(po: PathMatcher): Klaxon {
        pathMatchers.add(po)
        return this
    }

    val propertyStrategies = arrayListOf<PropertyStrategy>()

    fun propertyStrategy(ps: PropertyStrategy): Klaxon {
        propertyStrategies.add(ps)
        return this
    }
    /**
     * A map of a path to the JSON value that this path was found at.
     */
    private val allPaths = hashMapOf<String, Any>()

    inner class DefaultPathMatcher(private val paths: Set<String>) : PathMatcher {
        override fun pathMatches(path: String) : Boolean {
            return paths.contains(path)
        }
        override fun onMatch(path: String, value: Any) { allPaths[path] = value }
    }

    fun parser(kc: KClass<*>? = null, passedLexer: Lexer? = null, streaming: Boolean = false): Parser {
        val result = Annotations.findJsonPaths(kc)
        if (result.any()) {
            // If we found at least one @Json(path = ...), add the DefaultPathMatcher with a list
            // of all these paths we need to watch for (we don't want to do that if no path
            // matching was requested since matching these paths slows down parsing).
            pathMatchers.add(DefaultPathMatcher(result.toSet()))
        }

        return Parser.default(pathMatchers, passedLexer, streaming)
    }

    private val DEFAULT_CONVERTER = DefaultConverter(this, allPaths)

    private val converters = arrayListOf<Converter>(EnumConverter(), DEFAULT_CONVERTER)

    /**
     * Add a type converter. The converter is analyzed to find out which type it converts
     * and then that info is transferred to `converterMap`. Reflection is necessary to locate
     * the toJson() function since there is no way to define Converter in a totally generic
     * way that will compile.
     */
    fun converter(converter: Converter): Klaxon {
        converters.add(0, converter)
        return this
    }

    /**
     * Field type converters that convert fields with a marker annotation.
     */
    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, Converter>()

    fun fieldConverter(annotation: KClass<out Annotation>, converter: Converter): Klaxon {
        fieldTypeMap[annotation] = converter
        return this
    }

    var fieldRenamer: FieldRenamer? = null

    /**
     * Defines a field renamer.
     */
    fun fieldRenamer(renamer: FieldRenamer): Klaxon {
        fieldRenamer = renamer
        return this
    }
    /**
     * @return a converter that will turn `value` into a `JsonObject`. If a non-null property is
     * passed, inspect that property for annotations that would override the type converter
     * we need to use to convert it.
     */
    override fun findConverter(value: Any, prop: KProperty<*>?): Converter {
        val result = findConverterFromClass(value::class.java, prop)
        log("Value: $value, converter: $result")
        return result
    }

    /**
     * Given a Kotlin class and a property where the object should be stored, returns a `Converter` for that type.
     */
    fun findConverterFromClass(cls: Class<*>, prop: KProperty<*>?) : Converter {
        fun annotationsForProp(prop: KProperty<*>, kc: Class<*>): Array<out Annotation> {
            val result = kc.declaredFields.firstOrNull { it.name == prop.name }?.declaredAnnotations ?: arrayOf()

            return result
        }

        var propertyClass: Class<*>? = null
        val propConverter : Converter? =
            if (prop != null && prop.returnType.classifier is KClass<*>) {
                propertyClass = (prop.returnType.classifier as KClass<*>).java
                val dc = prop.getter.javaMethod?.declaringClass ?: prop.javaField?.declaringClass
                annotationsForProp(prop, dc!!).mapNotNull {
                    fieldTypeMap[it.annotationClass]
                }.firstOrNull()
            } else {
                null
            }

        val result = propConverter
                ?: findBestConverter(cls, prop)
                ?: (if (propertyClass != null) findBestConverter(propertyClass, prop) else null)
                ?: DEFAULT_CONVERTER
        // That last DEFAULT_CONVERTER above is not necessary since the default converter is part of the
        // list of converters by default and if all other converters fail, that one will apply
        // (since it is associated to the Object type), however, Kotlin doesn't know that and
        // will assume the result is nullable without it
        log("findConverterFromClass $cls returning $result")
        return result
    }

    private fun findBestConverter(cls: Class<*>, prop: KProperty<*>?) : Converter? {
        val toConvert = prop?.returnType?.javaType as? Class<*> ?: cls
        return converters.firstOrNull { it.canConvert(toConvert) }
    }

    fun toJsonString(value: Any?, prop: KProperty<*>? = null): String
            = if (value == null) "null" else toJsonString(value, findConverter(value, prop))

    private fun toJsonString(value: Any, converter: Any /* can be Converter or Converter */)
            : String {
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
        // If the user provided a type converter, use it, otherwise try to instantiate the object ourselves.
        val classConverter = findConverterFromClass(cls, null)
        val types = kc.typeParameters.map { KTypeProjection.invariant(it.createType()) }
        val type =
                if (kc.typeParameters.any()) kc.createType(types)
                else kc.createType()
        return classConverter.fromJson(JsonValue(jsonObject, cls, type, this@Klaxon)) as Any
    }

    /**
     * Convert the parameter into a JsonObject
     */
    @Suppress("unused")
    fun toJsonObject(obj: Any) = JsonValue.convertToJsonObject(obj, this)

    fun log(s: String) {
        if (Debug.verbose) println(s)
    }

}
