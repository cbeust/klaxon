package com.beust.klaxon

import java.lang.reflect.Field
import kotlin.reflect.KClass

class JsonConverter : JsonObjectConverter {
    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, Converter2<*>>()

    private val typeConverters = arrayListOf<Converter2<*>>(
        IntConverter(), StringConverter(), BooleanConverter(),
            ArrayConverter(this), ObjectConverter(this)
    )

    fun fieldTypeConverter(annotation: KClass<out Annotation>, adapter: Converter2<*>) {
        fieldTypeMap[annotation] = adapter
    }

    fun typeConverter(adapter: Converter2<*>) {
        // Note: always insert at the front of the list so that user defined
        // converters can override the default ones.
        typeConverters.add(0, adapter)
    }

    private fun log(s: String) = s //println(s)
    private fun warn(s: String) = "  WARNING: $s"

    fun <T> findBestConverter(obj: T) : Converter2<T>? {
        val jsonValue = JsonValue(obj, this)
        // Collect all the type converters
        val result = typeConverters.firstOrNull {
            val jv = it.fromJson(null, jsonValue)
            jv != null
        } as Converter2<T>
        return result
    }

    override fun toJsonString(p: Any?): String {
        val converter = findBestConverter(p)
        if (converter != null) {
            return converter.toJson(p!!)
        } else {
            throw KlaxonException("Don't know how to convert $p")
        }
    }

    override fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>): Any {
        fun setField(obj: Any, field: Field, value: Any) {
            field.isAccessible = true
            field.set(obj, value)
        }

        /**
         * Gather a list of all candidate type converters and take the first one that will convert
         * the value. @return null otherwise.
         */
        fun tryToConvert(field: Field, value: JsonValue) : Any? {
            val converters = arrayListOf<Converter2<*>>()
            // Collect all the field type converters
            converters.addAll(
                field.annotations.mapNotNull {
                    fieldTypeMap[it.annotationClass]
                })

            // Collect all the type converters
            typeConverters.firstOrNull { it.canConvert(field, value.inside) }?.let {
                converters.add(it)
            }

            // Take the first converter that will return a non-null value
            return converters.map {
                it.fromJson(field, value)
            }.firstOrNull()
        }

        println("Trying to instantiate $cls")
        if (cls.toString().contains("JsonObject")) {
            println("")
        }
        val result = cls.newInstance().apply {
            cls.declaredFields.forEach { field ->
                log("Looking at field: $field")
                val jsonAnnotation = field.getAnnotation(Json::class.java)
                val fieldName =
                    if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                    else field.name
                val jValue = jsonObject[fieldName]

                if (jValue == null) {
                    val jsonFields = jsonObject.keys.joinToString(",")
                    throw KlaxonException("Don't know how to map class field \"$fieldName\" " +
                            "to any JSON field: $jsonFields")
                } else {
                    val convertedValue = tryToConvert(field, JsonValue(jValue, this@JsonConverter))
                    if (convertedValue != null) {
                        setField(this, field, convertedValue)
                    } else {
                        val convertedValue = tryToConvert(field, JsonValue(jValue, this@JsonConverter))
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${field.type} for "
                                + "field named \"${field.name}\"")
                    }
                }
            }
        }
        return result
    }

}

interface JsonObjectConverter {
    fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>): Any
    fun toJsonString(p: Any?): String
}