package com.beust.klaxon

import com.beust.klaxon.internal.ConverterFinder
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

/**
 * Variant class that encapsulates one JSON value. Only exactly one of the property fields defined in the
 * constructor is guaranteed to be non null.
 */
class JsonValue constructor(value: Any?,
        val propertyClass: Type?,
        val propertyKClass: kotlin.reflect.KType?,
        converterFinder: ConverterFinder) {
    var obj: JsonObject? = null
    var array: JsonArray<*>? = null
    var string: String? = null
    var int: Int? = null
    private var bigDecimal: BigDecimal? = null
    private var bigInteger: BigInteger? = null
    var longValue: Long? = null
    var float: Float? = null
    var double: Double? = null
    private var char: Char? = null
    var boolean: Boolean? = null

    /**
     * If this object contains a JsonArray, @return the generic type of that array, null otherwise.
     */
    private var genericType: Class<*>? = null

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
            return when {
                obj != null -> obj
                array != null -> array
                string != null -> string
                int != null -> int
                longValue != null -> longValue
                float != null -> float
                double != null -> double
                char != null -> char
                boolean != null -> boolean
                bigDecimal != null -> bigDecimal
                bigInteger != null -> bigInteger
                else -> null
            }
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
                val v = JsonArray<Any?>()
                genericType = null
                value.forEach {
                    genericType = if (it == null) {
                        v.add(null)
                        Any::class.java
                    } else {
                        v.add(it)
                        it.javaClass
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
            is BigInteger -> {
                bigInteger = value
                type = BigInteger::class.java
            }
            is BigDecimal -> {
                bigDecimal = value
                type = BigDecimal::class.java
            }
            is Int -> {
                when(propertyKClass?.classifier) {
                    Float::class -> {
                        float = value.toFloat()
                        type = Float::class.java
                    }
                    Double::class -> {
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
                    obj = convertToJsonObject(value, converterFinder)
                    type = value.javaClass
                }
            }
        }
    }

    override fun toString() : String {
        val result = when {
            obj != null -> "{object: $obj"
            array != null -> "{array: $array"
            string != null -> "{string: $string"
            int != null -> "{int: $int"
            float != null -> "{float: $float"
            double != null -> "{double: $double"
            char != null -> "{char: $char"
            boolean != null -> "{boolean: $boolean"
            longValue != null -> "{longBalue: $longValue"
            else -> throw KlaxonException("Should never happen")
        }
        return "$result, property: $propertyKClass}"

    }

    companion object {
        fun convertToJsonObject(obj: Any, converterFinder: ConverterFinder = Klaxon()): JsonObject {
            val result = JsonObject()
            propertiesAndValues(obj).entries.forEach { entry ->
                val property = entry.key
                val p = entry.value
                result[property.name] = converterFinder.findConverter(p!!, property).toJson(p)
            }
            return result
        }

        private fun propertiesAndValues(obj: Any): Map<KProperty<*>, Any?> {
            val result = hashMapOf<KProperty<*>, Any?>()
            obj::class.declaredMemberProperties
//                    .filter { it.visibility != KVisibility.PRIVATE && it.isAccessible }
//            obj.javaClass.declaredFields
                    .forEach { property ->
                        val p = property.call(obj)
                        result[property] = p
                    }
            return result
        }
    }

    private fun error(type: String, name: String) : Nothing {
        throw KlaxonException("Couldn't find $type on object named $name")
    }
}
