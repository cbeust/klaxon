package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import java.io.*
import java.lang.reflect.ParameterizedType
import java.nio.charset.Charset
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

class Klaxon : ConverterFinder {
    /**
     * Parse a Reader into a JsonObject.
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
    inline fun <reified T> parse(json: String): T?
            = maybeParse(parser(T::class).parse(StringReader(json)) as JsonObject)

    /**
     * Parse a JSON string into a JsonArray.
     */
    inline fun <reified T> parseArray(json: String): List<T>?
            = parseFromJsonArray(parser(T::class).parse(StringReader(json)) as JsonArray<*>)

    /**
     * Parse a JSON file into an object.
     */
    inline fun <reified T> parse(file: File): T?
            = maybeParse(parser(T::class).parse(FileReader(file)) as JsonObject)

    /**
     * Parse an InputStream into an object.
     */
    inline fun <reified T> parse(inputStream: InputStream): T? {
        return maybeParse(parser(T::class).parse(toReader(inputStream)) as JsonObject)
    }

    /**
     * Parse a JsonReader into an array.
     */
    inline fun <reified T> parse(jsonReader: JsonReader): T? {
        val p = parser(T::class, jsonReader.lexer, streaming = true)
        return maybeParse(p.parse(jsonReader) as JsonObject)
    }

    /**
     * Parse a Reader into an object.
     */
    inline fun <reified T> parse(reader: Reader): T? {
        return maybeParse(parser(T::class).parse(reader) as JsonObject)
    }

    /**
     * Parse an InputStream into a JsonArray.
     */
    @Suppress("unused")
    inline fun <reified T> parseArray(inputStream: InputStream): List<T>? {
        return parseFromJsonArray(parser(T::class).parse(toReader(inputStream)) as JsonArray<*>)
    }

    /**
     * Parse a JsonObject into an object.
     */
    inline fun <reified T> parseFromJsonObject(map: JsonObject): T?
        = fromJsonObject(map, T::class.java, T::class) as T?

    inline fun <reified T> parseFromJsonArray(map: JsonArray<*>): List<T>? {
        val result = arrayListOf<Any>()
        map.forEach { jo ->
            if (jo is JsonObject) {
                val t = parseFromJsonObject<T>(jo)
                if (t != null) result.add(t)
                else throw KlaxonException("Couldn't convert $jo")
            } else if (jo != null) {
                val converter = findConverterFromClass(T::class.java, null)
                val convertedValue = converter.fromJson(JsonValue(jo, null, null, this))
                result.add(convertedValue!!)
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
            pathMatchers.add(DefaultPathMatcher(result.toSet()))
        }

        return Parser(pathMatchers, passedLexer, streaming)
    }

    private val DEFAULT_CONVERTER = DefaultConverter(this, allPaths)

    /**
     * Type converters that convert a JsonObject into an object.
     */
    private val converters = arrayListOf<Converter<*>>(DEFAULT_CONVERTER)
    private val converterMap = hashMapOf<java.lang.reflect.Type, Converter<*>>()

    fun converter(converter: Converter<*>): Klaxon {
        var type: java.lang.reflect.Type? = null
        converter::class.declaredFunctions.forEach { f ->
            if (f.name == "toJson") {
                type = f.parameters.firstOrNull { it.kind == KParameter.Kind.VALUE }?.type?.javaType
            }
        }
        converters.add(0, converter)
        if (type != null) {
            val c: java.lang.reflect.Type =
                    if (type is ParameterizedType) (type as ParameterizedType).rawType else type!!
            converterMap.put(c, converter)
//            converterMap[c] = converter
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
        val result = findConverterFromClass(value::class.java, prop)
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

        val result = propConverter
                ?: findBestConverter(jc)
                ?: (if (cls != null) findBestConverter(cls) else null)
                ?: DEFAULT_CONVERTER
        log("findConverterFromClass $jc returning $result")
        return result
    }

    private fun findBestConverter(cls: Class<*>) : Converter<*>? {
        val result = converterMap.entries.firstOrNull { entry ->
            val type = entry.key as Class<*>
            cls.isAssignableFrom(type)
        }
        return result?.value
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
        // If the user provided a type converter, use it, otherwise try to instantiate the object ourselves.
        val classConverter = findConverterFromClass(cls, null)
        val types = kc.typeParameters.map { KTypeProjection.invariant(it.createType()) }
        val type =
                if (kc.typeParameters.any()) kc.createType(types)
                else kc.createType()
        return classConverter.fromJson(JsonValue(jsonObject, cls, type, this@Klaxon)) as Any
    }

    fun log(s: String) {
        if (Debug.verbose) println(s)
    }
}
