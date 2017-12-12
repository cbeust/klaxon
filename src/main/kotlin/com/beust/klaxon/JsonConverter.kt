package com.beust.klaxon

import java.lang.reflect.Field
import kotlin.reflect.KClass

class JsonConverter : JsonObjectConverter {
    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, TypeConverter<*>>()

    private val typeConverters = arrayListOf<TypeConverter<*>>(
        IntConverter(), StringConverter(), LongConverter(), BooleanConverter(),
            ArrayConverter(this), ObjectConverter(this)
    )

    fun fieldTypeConverter(annotation: KClass<out Annotation>, adapter: TypeConverter<*>) {
        fieldTypeMap[annotation] = adapter
    }

    fun typeConverter(adapter: TypeConverter<*>) {
        // Note: always insert at the front of the list so that user defined
        // converters can override the default ones.
        typeConverters.add(0, adapter)
    }

    private fun log(s: String) = s //println(s)
    private fun warn(s: String) = "  WARNING: $s"

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
            val converters = arrayListOf<TypeConverter<*>>()
            // Collect all the field type converters
            converters.addAll(
                field.annotations.mapNotNull {
                    fieldTypeMap[it.annotationClass]
                })

            // Collect all the type converters
            typeConverters.firstOrNull { it.fromJson(field, value) != null }?.let {
                converters.add(it)
            }

            // Take the first converter that will return a non-null value
            return converters.map {
                it.fromJson(field, value)
            }.firstOrNull()
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
                    val convertedValue = tryToConvert(field, JsonValue(jValue))
                    if (convertedValue != null) {
                        setField(this, field, convertedValue)
                    } else {
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
}