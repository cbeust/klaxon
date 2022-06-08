@file:Suppress("UnnecessaryVariable")

package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.jvm.jvmErasure

/**
 * The default Klaxon converter, which attempts to convert the given value as an enum first and if this fails,
 * using reflection to enumerate the fields of the passed object and assign them values.
 */
class DefaultConverter(private val klaxon: Klaxon, private val allPaths: HashMap<String, Any>) : Converter {
    override fun canConvert(cls: Class<*>) = true

    override fun fromJson(jv: JsonValue): Any? {
        val value = jv.inside
        val propertyType = jv.propertyClass
        val classifier = jv.propertyKClass?.classifier
        val result =
            when(value) {
                is Boolean, is String -> value
                is Int -> fromInt(value, propertyType)
                is BigInteger, is BigDecimal -> value
                is Double ->
                    if (classifier == Float::class) fromFloat(value.toFloat(), propertyType)
                    else fromDouble(value, propertyType)
                is Float ->
                    if (classifier == Double::class) fromDouble(value.toDouble(), propertyType)
                    else fromFloat(value, propertyType)
                is Long ->
                    when (classifier) {
                        Double::class -> fromDouble(value.toDouble(), propertyType)
                        Float::class -> fromFloat(value.toFloat(), propertyType)
                        else -> value
                    }
                is Collection<*> -> fromCollection(value, jv)
                is JsonObject -> fromJsonObject(value, jv)
                null -> null
                else -> {
                    throw KlaxonException("Don't know how to convert $value")
                }
            }
        return result

    }

    override fun toJson(value: Any): String {
        fun joinToString(list: Collection<*>, open: String, close: String)
            = open + list.joinToString(", ") + close

        val result = when (value) {
            is String, is Enum<*> -> "\"" + Render.escapeString(value.toString()) + "\""
            is Double, is Float, is Int, is Boolean, is Long -> value.toString()
            is Array<*> -> {
                val elements = value.map { klaxon.toJsonString(it) }
                joinToString(elements, "[", "]")
            }
            is Collection<*> -> {
                val elements = value.map { klaxon.toJsonString(it) }
                joinToString(elements, "[", "]")
            }
            is Map<*, *> -> {
                val valueList = arrayListOf<String>()
                value.entries.forEach { entry ->
                    val jsonValue =
                        if (entry.value == null) "null"
                        else klaxon.toJsonString(entry.value as Any)
                    valueList.add("\"${entry.key}\": $jsonValue")
                }
                joinToString(valueList, "{", "}")
            }
            is BigInteger -> value.toString()
            else -> {
                val valueList = arrayListOf<String>()
                val properties = Annotations.findNonIgnoredProperties(value::class, klaxon.propertyStrategies)
                properties.forEach { prop ->
                    val getValue = prop.getter.call(value)
                    val getAnnotation = Annotations.findJsonAnnotation(value::class, prop.name)

                    // Use instance settings only when no local settings exist
                    if (getValue != null
                        || (getAnnotation?.serializeNull == true) // Local settings have precedence to instance settings
                        || (getAnnotation == null && klaxon.instanceSettings.serializeNull)
                    ) {
                            val jsonValue = klaxon.toJsonString(getValue, prop)
                            val fieldName = Annotations.retrieveJsonFieldName(klaxon, value::class, prop)
                            valueList.add("\"$fieldName\" : $jsonValue")
                        }
                }
                joinToString(valueList, "{", "}")
            }

        }
        return result
    }

    private fun fromInt(value: Int, propertyType: java.lang.reflect.Type?): Any {
        // If the value is an Int and the property is a Long, widen it
        val isLong = java.lang.Long::class.java == propertyType || Long::class.java == propertyType
        val result: Any = when {
            isLong -> value.toLong()
            propertyType == BigDecimal::class.java -> BigDecimal(value)
            else -> value
        }
        return result
    }

    private fun fromDouble(value: Double, propertyType: java.lang.reflect.Type?): Any {
        return if (propertyType == BigDecimal::class.java) {
            BigDecimal(value)
        } else {
            value
        }
    }

    private fun fromFloat(value: Float, propertyType: java.lang.reflect.Type?): Any {
        return if (propertyType == BigDecimal::class.java) {
            BigDecimal(value.toDouble())
        } else {
            value
        }
    }

    private fun fromCollection(value: Collection<*>, jv: JsonValue): Any {
        val kt = jv.propertyKClass
        val jt = jv.propertyClass
        val convertedCollection = value.map {
            // Try to find a converter for the element type of the collection
            if (jt is ParameterizedType) {
                val typeArgument = jt.actualTypeArguments[0]
                val converter =
                    when (typeArgument) {
                        is Class<*> -> klaxon.findConverterFromClass(typeArgument, null)
                        is ParameterizedType -> {
                            when (val ta = typeArgument.actualTypeArguments[0]) {
                                is Class<*> -> klaxon.findConverterFromClass(ta, null)
                                is ParameterizedType -> klaxon.findConverterFromClass(ta.rawType.javaClass, null)
                                else -> throw KlaxonException("SHOULD NEVER HAPPEN")
                            }
                        }
                        else -> throw IllegalArgumentException("Should never happen")
                    }
                val kTypeArgument = kt?.arguments!![0].type
                converter.fromJson(JsonValue(it, typeArgument, kTypeArgument, klaxon))
            } else {
                if (it != null) {
                    val converter = klaxon.findConverter(it)
                    converter.fromJson(JsonValue(it, jt, kt, klaxon))
                } else {
                    throw KlaxonException("Don't know how to convert null value in array $jv")
                }
            }

        }

        val result =
                when {
                    Annotations.isSet(jt) -> {
                        convertedCollection.toSet()
                    }
                    Annotations.isArray(kt) -> {
                        val componentType = (jt as Class<*>).componentType
                        val array = java.lang.reflect.Array.newInstance(componentType, convertedCollection.size)
                        convertedCollection.indices.forEach { i ->
                            java.lang.reflect.Array.set(array, i, convertedCollection[i])
                        }
                        array
                    }
                    else -> {
                        convertedCollection
                    }
                }
        return result
    }

    private fun fromJsonObject(value: JsonObject, jv: JsonValue): Any {
        val jt = jv.propertyClass
        val result =
            if (jt is ParameterizedType) {
                val isMap = Map::class.java.isAssignableFrom(jt.rawType as Class<*>)
                val isCollection = List::class.java.isAssignableFrom(jt.rawType as Class<*>)
                when {
                    isMap -> {
                        // Map
                        val result = linkedMapOf<String, Any?>()
                        value.entries.forEach { kv ->
                            val key = kv.key
                            kv.value?.let { mv ->
                                val typeValue = jt.actualTypeArguments[1]
                                val converter = klaxon.findConverterFromClass(
                                        typeValue.javaClass, null)
                                val convertedValue = converter.fromJson(
                                        JsonValue(mv, typeValue, jv.propertyKClass!!.arguments[1].type,
                                                klaxon))
                                result[key] = convertedValue
                            }
                        }
                        result
                    }
                    isCollection -> {
                        when(val type =jt.actualTypeArguments[0]) {
                            is Class<*> -> {
                                val cls = jt.actualTypeArguments[0] as Class<*>
                                klaxon.fromJsonObject(value, cls, cls.kotlin)
                            }
                            is ParameterizedType -> {
                                val result2 = JsonObjectConverter(klaxon, HashMap()).fromJson(value,
                                        jv.propertyKClass!!.jvmErasure)
                                result2

                            }
                            else -> {
                                throw IllegalArgumentException("Couldn't interpret type $type")
                            }
                        }
                    }
                    else -> throw KlaxonException("Don't know how to convert the JsonObject with the following keys" +
                            ":\n  $value")
                }
            } else {
                if (jt is Class<*>) {
                    if (jt.isArray) {
                        val typeValue = jt.componentType
                        klaxon.fromJsonObject(value, typeValue, typeValue.kotlin)
                    } else {
                        JsonObjectConverter(klaxon, allPaths).fromJson(jv.obj!!, jv.propertyKClass!!.jvmErasure)
                    }
                } else {
                    val typeName: Any? =
                        if (jt is TypeVariable<*>) {
                            jt.genericDeclaration
                        } else {
                            jt
                        }
                    throw IllegalArgumentException("Generic type not supported: $typeName")
                }
            }
        return result
    }

}
