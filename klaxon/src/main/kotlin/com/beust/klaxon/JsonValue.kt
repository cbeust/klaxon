package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import java.lang.reflect.Type
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

/**
 * Variant class that encapsulates one JSON value. Only exactly one of the property fields defined in the
 * constructor is guaranteed to be non null.
 */
class JsonValue(value: Any?,
        val propertyClass: Type?,
        val propertyKClass: kotlin.reflect.KType?,
        private val converterFinder: ConverterFinder) {
    var obj: JsonObject? = null
    var array: JsonArray<*>? = null
    var string: String? = null
    var int: Int? = null
    var longValue: Long? = null
    var float: Float? = null
    var double: Double? = null
    var char: Char? = null
    var boolean: Boolean? = null

    /**
     * If this object contains a JsonArray, @return the generic type of that array, null otherwise.
     */
    var genericType: Class<*>? = null

    var type: Class<*>

    /**
     * Convenience function to retrieve an Int value from the underlying `obj` field.
     */
    fun objInt(name: String) : Int  = obj?.int(name) ?: error("Int", name)

    /**
     * Convenience function to retrieve a String value from the underlying `obj` field.
     */
    fun objString(name: String) : String = obj?.string(name) ?: error("String", name)

    /**
     * @return the raw value inside this object.
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    val inside: Any?
        get() {
            val result =
                when {
                    obj != null -> obj
                    array != null -> array
                    string != null -> string
                    int != null -> int
                    longValue != null -> longValue
                    float != null -> float
                    double != null -> double
                    char != null -> char
                    boolean != null -> boolean
                    else -> null
                }
            return result
        }


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
                when(propertyKClass?.classifier) {
                    kotlin.Float::class -> {
                        float = value.toFloat()
                        type = Float::class.java
                    }
                    kotlin.Double::class -> {
                        double = value.toDouble()
                        type = Double::class.java
                    }
                    else -> {
                        int = value
                        type = Int::class.java

                    }
                }
            }
            is Long -> {
                longValue = value
                type = Long::class.java
            }
            is Double -> {
                double = value
                type = Double::class.java
            }
            is Float -> {
                double = value.toDouble()
                type = Double::class.java
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
                    obj = null
                    type = Any::class.java
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

    override fun toString() : String {
        val result = if (obj != null) "{object: $obj"
            else if (array != null) "{array: $array"
            else if (string != null) "{string: $string"
            else if (int != null) "{int: $int"
            else if (float != null) "{float: $float"
            else if (double != null) "{double: $double"
            else if (char != null) "{char: $char"
            else if (boolean != null) "{boolean: $boolean"
            else throw KlaxonException("Should never happen")
        return result + ", property: " + propertyKClass + "}"

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

    private fun error(type: String, name: String) : Nothing {
        throw KlaxonException("Couldn't find $type on object named $name")
    }
}