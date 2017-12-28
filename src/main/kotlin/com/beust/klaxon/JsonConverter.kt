package com.beust.klaxon

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun annotationsForProp(prop: KProperty<*>, kc: Class<*>): Array<out Annotation> {
    val result = kc.getDeclaredField(prop.name)?.declaredAnnotations ?: arrayOf()
    return result
}

fun annotationForProp(prop: KProperty<*>, kc: Class<*>, annotation: KClass<out Annotation>)
    = kc.getDeclaredField(prop.name).getDeclaredAnnotation(annotation.java)

class JsonConverter : JsonObjectConverter {
    private val fieldTypeMap = hashMapOf<KClass<out Annotation>, Converter2>()

    private val typeConverters = arrayListOf(
        _BooleanConverter(),
        _IntConverter(),
        _StringConverter()
        ,
        _ArrayConverter(this),
        _ObjectConverter(this)
    )

    fun toJson(value: Any) : String {
        val c = typeConverters.firstOrNull() { it.toJson("foo") != null }
        val converter = typeConverters.firstOrNull() { it.toJson(value) != null }
        val result =
                if (converter != null) {
                    val json = converter.toJson(value)
                    json
                } else {
                    throw KlaxonException("Couldn't find a converter for $value")
                }
        return result
    }

    fun fieldTypeConverter(annotation: KClass<out Annotation>, adapter: Converter2) {
        fieldTypeMap[annotation] = adapter
    }

    fun typeConverter(adapter: Converter2) {
        // Note: always insert at the front of the list so that user defined
        // converters can override the default ones.
        typeConverters.add(0, adapter)
    }

    private fun log(s: String) = println(s) //println(s)
    private fun warn(s: String) = "  WARNING: $s"

    fun findBestConverter(obj: Any) : Converter2? {
        return null
//        val jsonValue = JsonValue(obj, this, null)
//        // Collect all the type converters
//        val result = typeConverters.firstOrNull {
//            it.fromJson(null, jsonValue) != null
//        }
//        if (result == null) {
//            throw KlaxonException("Couldn't find a converter for $obj")
//        }
//        return result as Converter2
    }

    override fun toJsonString(p: Any?): String {
        return "FOO"
//        val converter = findBestConverter(p!!)
//        if (converter != null) {
//            return converter.toJson(p!!)
//        } else {
//            throw KlaxonException("Don't know how to convert $p")
//        }
    }

    override fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>, kc: KClass<*>?): Any {
        fun setField(obj: Any, prop: KProperty<*>, value: Any) {
            if (prop is KMutableProperty<*>) {
                prop.setter.call(obj, value)
            } else {
                throw KlaxonException("Property $prop is not mutable")
            }
        }

        /**
         * Gather a list of all candidate type converters and take the first one that will convert
         * the value. @return null otherwise.
         */
        fun tryToConvert(prop: KProperty1<out Any, Any?>, value: Any) : Any? {
//            val converters = arrayListOf<Converter2>()
//            // Collect all the prop type converters
//            converters.addAll(
//                annotationsForProp(prop, cls).mapNotNull {
//                    val annotation = annotationForProp(prop, cls, it.annotationClass)
//                    fieldTypeMap[it.annotationClass]
//                })
//
//            // Collect all the type converters
//            typeConverters.firstOrNull { it.fromJson(prop, JsonValue(value, this, null)) != null }?.let {
//                converters.add(it)
//            }
//
//            // Take the first converter that will return a non-null value
//            val result = converters.map {
//                it.fromJson(prop, JsonValue(value, this, null))
//            }.firstOrNull()
//            return result
            return null
        }

        println("Trying to instantiate $cls")
        if (cls.toString().contains("JsonObject")) {
            println("")
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
                    val convertedValue = tryToConvert(prop, jValue)
                    if (convertedValue != null) {
                        setField(this, prop, convertedValue)
                    } else {
                        val convertedValue = tryToConvert(prop, jValue)
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${prop::class} for "
                                + "field named \"${prop.name}\"")
                    }
                }

            }
//            cls.declaredFields.forEach { field ->
//                log("Looking at field: $field")
//                val jsonAnnotation = field.getAnnotation(Json::class.java)
//                val fieldName =
//                    if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
//                    else field.name
//                val jValue = jsonObject[fieldName]
//
//                if (jValue == null) {
//                    val jsonFields = jsonObject.keys.joinToString(",")
//                    throw KlaxonException("Don't know how to map class field \"$fieldName\" " +
//                            "to any JSON field: $jsonFields")
//                } else {
//                    val convertedValue = tryToConvert(field, jValue)
//                    if (convertedValue != null) {
//                        setField(this, field, convertedValue)
//                    } else {
//                        val convertedValue = tryToConvert(field, jValue)
//                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${field.type} for "
//                                + "field named \"${field.name}\"")
//                    }
//                }
//            }
        }
        return result
    }

}

interface JsonObjectConverter {
    fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>, kc: KClass<*>?): Any
    fun toJsonString(p: Any?): String
}