package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType

/**
 * The default Klaxon converter, which attempts to convert the given value as an enum first and if this fails,
 * using reflection to enumerate the fields of the passed object and assign them values.
 */
class DefaultConverter(private val klaxon: Klaxon) : Converter<Any> {
    override fun fromJson(jv: JsonValue): Any
            = maybeConvertEnum(jv) ?: convertValue(jv)

    override fun toJson(value: Any): String? {
        val result = when (value) {
            is String -> "\"" + value + "\""
            is Int, is Boolean, is Long -> value.toString()
            is Collection<*> -> {
                val elements = value.filterNotNull().map { toJson(it) }
                "[" + elements.joinToString(", ") + "]"
            }
            else -> {
                val valueList = arrayListOf<String>()
                value::class.declaredMemberProperties.forEach { prop ->
                    prop.getter.call(value)?.let { getValue ->
                        val jsonValue = klaxon.toJsonString(getValue)
                        valueList.add("\"${prop.name}\" : $jsonValue")
                    }
                }
                return "{" + valueList.joinToString(", ") + "}"
            }

        }
        return result
    }

    private fun maybeConvertEnum(jv: JsonValue): Any? {
        var result: Any? = null
        jv.property?.let { property ->
            val cls = property.returnType.javaType
            if (cls is Class<*> && cls.isEnum) {
                val valueOf = cls.getMethod("valueOf", String::class.java)
                result = valueOf.invoke(null, jv.inside)
            }
        }

        return result
    }

    private fun convertValue(jv: JsonValue) : Any {
        val value = jv.inside
        val result = when(value) {
            is String -> value
            is Boolean -> value
            is Int -> {
                // If the value is an Int and the property is a Long, widen the value
                val propertyType = jv.property?.getter?.returnType?.javaType as? Class<*>
                val isLong = java.lang.Long::class.java == propertyType || Long::class.java == propertyType
                if (isLong) value.toLong() else value
            }
            is Long -> value
            is Collection<*> -> value.map {
                val jt = jv.property?.returnType?.javaType
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

                converter.fromJson(JsonValue(it, jv.property, klaxon))
            }
            is JsonObject -> {
                val jt = jv.property?.returnType?.javaType
                when (jt) {
                    is ParameterizedType -> {
                        val cls = jt.actualTypeArguments[0] as Class<*>
                        klaxon.fromJsonObject(value, cls, cls.kotlin)
                    }
                    is Class<*> -> {
                        val cls = jv.property.getter.returnType.javaType as Class<*>
                        klaxon.fromJsonObject(value, cls, cls.kotlin)
                    }
                    else -> {
                        throw KlaxonException("Don't know how to convert $value")
                    }
                }
            }
            else -> {
                throw KlaxonException("Don't know how to convert $value")
            }
        }
        return result
    }

}