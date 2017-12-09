package com.beust.klaxon

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.io.StringReader
import java.lang.reflect.Field
import kotlin.reflect.KClass

class JsonAdapter {
    private val typeMap = hashMapOf<KClass<out Annotation>, KlaxonAdapter<*>>()

    fun typeAdapter(annotation: KClass<out Annotation>, adapter: KlaxonAdapter<*>) {
        typeMap[annotation] = adapter
    }

    companion object {
        private val PRIMITIVES = setOf(
                Integer::class.java, String::class.java, Long::class.java, Float::class.java,
                Double::class.java, Character::class.java, java.lang.Boolean::class.java)

        private fun isPrimitive(type: Class<*>) = type.isPrimitive || type in PRIMITIVES
    }

    inline fun <reified T>fromJson(json: String) : T? {
        val map = Parser().parse(StringReader(json))
        if (map is JsonObject) {
            val cls = T::class.java
            return fromJsonObject(map, cls) as T?
        } else {
            return null
        }
    }

    private fun warn(s: String) = "  WARNING: $s"

    fun fromJsonObject(jsonObject: JsonObject, cls: Class<*>): Any {
        fun setField(obj: Any, field: Field, value: Any) {
            field.isAccessible = true
            field.set(obj, value)
        }

        fun findBestTypeAdapters(field: Field) : List<KlaxonAdapter<*>> {
            val result = arrayListOf<KlaxonAdapter<*>>()
            result.addAll(
                field.annotations.mapNotNull {
                    typeMap[it.annotationClass]
                })
            return result
        }

        val result = cls.newInstance().apply {
            cls.declaredFields.forEach { field ->
                println("Looking at field: $field")
                val jsonAnnotation = field.getAnnotation(Json::class.java)
                val fieldName =
                    if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                    else field.name
                val jValue = jsonObject[fieldName]
                if (jValue == null) {
                    throw KlaxonException("Don't know how to map \"$fieldName\" to field \"${field.name}\"")
                } else {
                    val typeAdapters = findBestTypeAdapters(field)

                    var foundAdapter = false
                    typeAdapters.forEach { typeAdapter ->
                        val adapted = typeAdapter.fromJson(JsonValue(jValue))
                        if (adapted != null) {
                            setField(this, field, adapted)
                            foundAdapter = true
                        }
                    }

                    if (! foundAdapter) {
                        if (isPrimitive(field.type)) {
                            if (field.type.isAssignableFrom(jValue.javaClass)) {
                                println("  Found value: $jValue")
                                setField(this, field, jValue)
                            } else {
                                println(warn("  Found value with incompatible type: "
                                        + " value $jValue, expected " + field.type
                                        + " found KlaxonJson : " + jValue.javaClass))
                            }
                        } else if (jValue is JsonArray<*>) {
                            val fieldType = field.type
                            if (fieldType.isAssignableFrom(List::class.java)) {
                                val elementType = (field.genericType as ParameterizedTypeImpl).actualTypeArguments[0]
                                val newField = arrayListOf<Any?>()
                                jValue.forEach {
                                    if (it is JsonObject) {
                                        val inflated = fromJsonObject(it, elementType as Class<*>)
                                        newField.add(inflated)
                                    } else {
                                        newField.add(it)
                                    }
                                }
                                setField(this, field, newField)
                            }
                        } else if (jValue is JsonObject) {
                            val fieldType = field.type
                            val newField = fromJsonObject(jValue, fieldType)

                            setField(this, field, newField)
                        } else {
                            throw KlaxonException("Don't know how to convert \"$jValue\" into ${field.type} for "
                                    + "field named \"${field.name}\"")
                        }
                    }
                }
            }
        }
        return result
    }

}
