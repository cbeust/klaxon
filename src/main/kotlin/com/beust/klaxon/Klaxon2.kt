package com.beust.klaxon

import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

interface Converter2<T> {
    fun canConvert(field: Field?, value: Any) : Boolean
    fun fromJson(field: Field?, jValue: JsonValue) : T?
    fun toJson(value: Any) : String
}

class _IntConverter: Converter2<Int> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == Int::class.java || value::class == Int::class.java
            || field?.type == Integer::class.java || value::class == Integer::class
    }

    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Integer::class.java)) value.int else null

    override fun toJson(value: Any): String {
        return value.toString()
    }

}

class _BooleanConverter: Converter2<Boolean> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == Boolean::class.java || value::class == Boolean::class.java
           || field?.type == Boolean::class || value::class == Boolean::class
    }

    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Boolean::class.java)) value.boolean else null

    override fun toJson(value: Any): String {
        return value.toString()
    }

}

class _StringConverter: Converter2<String> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == String::class.java || value::class.java == String::class.java
    }

    override fun fromJson(field: Field?, jValue: JsonValue): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toJson(value: Any): String {
        return "\"" + value.toString() + "\""
    }

}

class _ArrayConverter(val parent: Klaxon2): Converter2<Array<*>> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return value is Collection<*>
    }

    override fun fromJson(field: Field?, jValue: JsonValue): Array<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toJson(value: Any): String {
        val array = value as Collection<*>
        val result = StringBuffer()
        var first = true
        array.forEach {
            if (it != null) {
                if (! first) result.append(", ")
                first = false
                result.append(parent.toJson(it))
            }
        }
        return "[" + result.toString() + "]"
    }
}

class _ObjectConverter(val parent: Klaxon2): Converter2<Array<*>> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return true
    }

    override fun fromJson(field: Field?, jValue: JsonValue): Array<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toJson(value: Any): String {
        val sb = StringBuilder()
        var first = true
        value::class.declaredMemberProperties
                .filter { it.visibility != KVisibility.PRIVATE }
                .forEach { property ->
            val p = property.getter.call(value)
            if (! first) sb.append(", ")
            first = false
            sb.append("\"" + property.name + "\" : ")
            sb.append(parent.toJson(p!!))
        }
        return "{ " + sb.toString() + "}"
    }

}


class Klaxon2 {
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

    val converters = arrayOf(
            _BooleanConverter(),
            _IntConverter(),
            _StringConverter(),
            _ArrayConverter(this),
            _ObjectConverter(this)
    )

    fun toJson(value: Any) : String {
        val c = converters.firstOrNull() { it.canConvert(null, "foo") }
        val converter = converters.firstOrNull() { it.canConvert(null, value) }
        val result =
            if (converter != null) {
                val json = converter.toJson(value)
                json
            } else {
                throw KlaxonException("Couldn't find a converter for $value")
            }
        return result
    }
}