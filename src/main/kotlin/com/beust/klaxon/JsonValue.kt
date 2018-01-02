package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

/**
 * Variant class that encapsulates one JSON value.
 */
class JsonValue(value: Any?, val property: KProperty<*>?, private val converterFinder: ConverterFinder) {
    var obj: JsonObject? = null
    var array: JsonArray<*>? = null
    var string: String? = null
    var int: Int? = null
    var float: Float? = null
    var char: Char? = null
    var boolean: Boolean? = null
    var genericType: Class<*>? = null

    var type: Class<*>

    private fun error(type: String, name: String) : Nothing {
        throw KlaxonException("Couldn't find $type on object named $name")
    }

    fun objInt(name: String) : Int  = obj?.int(name) ?: error("Int", name)
    fun objString(name: String) : String = obj?.string(name) ?: error("String", name)

    init {
        when(value) {
            is JsonValue -> {
                type = String::class.java
            }
            is JsonObject -> {
                obj = value
                type = value.javaClass
            }
            is Collection<*> -> {
                val v = JsonArray<Any>()
                genericType = null
                value.forEach {
                    if (it is Any) {
                        v.add(it)
                        genericType = it.javaClass
                    } else {
                        throw KlaxonException("Need to extract inside")
                    }
                }
                array = v
                type = List::class.java
            }

            is JsonArray<*> -> {
                array = value
                type = List::class.java
            }
            is String -> {
                string = value
                type = String::class.java
            }
            is Int -> {
                int = value
                type = Int::class.java
            }
            is Float -> {
                float = value
                type = Float::class.java
            }
            is Char -> {
                char = value
                type = Char::class.java
            }
            is Boolean -> {
                boolean = value
                type = Boolean::class.java
            }
            else -> {
                if (value == null) {
                    throw IllegalArgumentException("Should never be null")
                } else {
                    obj = convertToJsonObject(value)
                    type = value.javaClass
                }
            }
        }
    }

    private fun convertToJsonObject(obj: Any): JsonObject {
        val result = JsonObject()
        propertiesAndValues(obj).entries.forEach { entry ->
            val property = entry.key
            val p = entry.value
            result[property.name] = converterFinder.findConverter(p!!, property)
        }
        return result
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    val inside: Any
        get() {
            val result =
                if (obj != null) obj
                    else if (array != null) array
                    else if (string != null) string
                    else if (int != null) int
                    else if (float != null) float
                    else if (char != null) char
                    else if (boolean != null) boolean
                    else throw KlaxonException("Should never happen")
            return result!!
        }

    override fun toString() : String {
        return if (obj != null) "{object: $obj}"
            else if (array != null) "{array: $array}"
            else if (string != null) "{string: $string}"
            else if (int != null) "{int: $int}"
            else if (float != null) "{float: $float}"
            else if (char != null) "{char: $char}"
            else if (boolean != null) "{boolean: $boolean}"
            else throw KlaxonException("Should never happen")

    }

    companion object {
        fun propertiesAndValues(obj: Any): Map<KProperty<*>, Any?> {
            val result = hashMapOf<KProperty<*>, Any?>()
            obj::class.declaredMemberProperties
//                    .filter { it.visibility != KVisibility.PRIVATE && it.isAccessible }
//            obj.javaClass.declaredFields
                    .forEach { property ->
                        val p = property.call(obj)
                        result.put(property, p)
                    }
            return result
        }
    }

}