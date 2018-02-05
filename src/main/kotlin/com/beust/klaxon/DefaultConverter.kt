package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * The default Klaxon converter, which attempts to convert the given value as an enum first and if this fails,
 * using reflection to enumerate the fields of the passed object and assign them values.
 */
class DefaultConverter(private val klaxon: Klaxon) : Converter<Any> {
    override fun fromJson(jv: JsonValue): Any
            = maybeConvertEnum(jv) ?: convertValue(jv)

    override fun toJson(value: Any): String? {
        fun joinToString(list: Collection<*>, open: String, close: String)
            = open + list.joinToString(", ") + close

        val result = when (value) {
            is String, is Enum<*> -> "\"" + Render.escapeString(value.toString()) + "\""
            is Double, is Int, is Boolean, is Long -> value.toString()
            is Collection<*> -> {
                val elements = value.filterNotNull().map { klaxon.toJsonString(it) }
                joinToString(elements, "[", "]")
            }
            is Map<*, *> -> {
                val valueList = arrayListOf<String>()
                value.entries.forEach { entry ->
                    val jsonValue = klaxon.toJsonString(entry.value as Any)
                    valueList.add("\"${entry.key}\": $jsonValue")
                }
                joinToString(valueList, "{", "}")
            }
            else -> {
                val valueList = arrayListOf<String>()
                val properties = Annotations.findNonIgnoredProperties(value::class)
                if (properties.isNotEmpty()) {
                    properties.forEach { prop ->
                        prop.getter.call(value)?.let { getValue ->
                            val jsonValue = klaxon.toJsonString(getValue)
                            val jsonFieldName = Annotations.findJsonAnnotation(value::class, prop.name)?.name
                            val fieldName =
                                    if (jsonFieldName != null && jsonFieldName != "") jsonFieldName
                                    else prop.name
                            valueList.add("\"$fieldName\" : $jsonValue")
                        }
                    }
                    joinToString(valueList, "{", "}")
                } else {
                    """"$value""""
                }
            }

        }
        return result
    }

    private fun maybeConvertEnum(jv: JsonValue): Any? {
        var result: Any? = null
//        jv.property?.let { property ->
//            val cls = property.returnType.javaType
            if (jv.propertyClass is Class<*> && jv.propertyClass.isEnum) {
                val valueOf = jv.propertyClass.getMethod("valueOf", String::class.java)
                result = valueOf.invoke(null, jv.inside)
            }
//        }

        return result
    }

    private fun convertValue(jv: JsonValue) : Any {
        val value = jv.inside
        val result =
            if (value is Int) {
                // If the value is an Int and the property is a Long, widen it
                val propertyType = jv.propertyClass
                val isLong = java.lang.Long::class.java == propertyType || Long::class.java == propertyType
                if (isLong) value.toLong() else value
            } else if (value is Boolean || value is String || value is Double || value is Long) {
                value
            } else if (value is Float) {
                value.toDouble()
            } else if (value is Collection<*>) {
                value.map {
                    val jt = jv.propertyClass
                    // Try to find a converter for the element type of the collection
                    val converter =
                            if (jt is ParameterizedType) {
                                val cls = jt.actualTypeArguments[0] as Class<*>
                                klaxon.findConverterFromClass(cls, null)
                            } else {
                                if (it != null) {
                                    klaxon.findConverter(it)
                                } else {
                                    throw KlaxonException("Don't know how to convert null value in array $jv")
                                }
                            }

                    converter.fromJson(JsonValue(it, jv.propertyClass, klaxon))
                }
            } else if (value is JsonObject) {
                val jt = jv.propertyClass
                if (jt!!.javaType is ParameterizedType) {
                    val rawType = (jt.javaType as ParameterizedType).rawType
                    val isMap = (rawType as Class<*>).isAssignableFrom(AbstractMap::class.java)
                    val isCollection = (rawType as Class<*>).isAssignableFrom(Collection::class.java)
                    if (isMap) {
                        // Map
                        val result = linkedMapOf<String, Any>()
                        value.entries.forEach { kv ->
                            val key = kv.key
                            kv.value?.let { mv ->
                                val vc = mv::class.java
                                val typeValue = (jt.javaType as ParameterizedType).actualTypeArguments[1]
                                val converter = klaxon.findConverterFromClass(
                                        typeValue.javaClass, null)
                                val convertedValue = converter.fromJson(
                                        JsonValue(mv, jt.arguments[1].type, klaxon))
                                if (convertedValue != null) {
                                    result[key] = convertedValue
                                }
                            }
                        }
                        result
                    } else if (isCollection) {
                        println("PROBLEM")
//                        val cls = (jt as KClass<*>).java.actualTypeArguments[0] as Class<*>
//                        klaxon.fromJsonObject(value, cls, cls.kotlin)
                    } else {
                        throw KlaxonException("Don't know how to convert the JsonObject with the following keys" +
                                ":\n  $value")
                    }
                } else {
                    val result = klaxon.fromJsonObject(value, jt.javaType as Class<*>, jt.jvmErasure)
                    result!!
                }
            } else {
                throw KlaxonException("Don't know how to convert $value")
            }
        return result
    }

}