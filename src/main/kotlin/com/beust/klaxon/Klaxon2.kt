package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

interface Converter2 {
//    fun canConvert(field: KProperty<*>?, value: Any) : Boolean
    fun fromJson(field: KProperty<*>?, jValue: JsonValue) : Any?
    fun toJson(value: Any) : String
}

class _IntConverter: Converter2 {
//    override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//        return field?.returnType == Int::class.java || value::class == Int::class.java
//            || field?.returnType == Integer::class.java || value::class == Integer::class
//    }

    override fun fromJson(field: KProperty<*>?, value: JsonValue)
            = if (value.inside is Int || value.inside is Integer) value.int else null

    override fun toJson(value: Any): String {
        return value.toString()
    }

}

class _BooleanConverter: Converter2 {
//    override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//        return field?.returnType == Boolean::class.java || value::class == Boolean::class.java
//           || field?.returnType == Boolean::class || value::class == Boolean::class
//    }

    override fun fromJson(field: KProperty<*>?, value: JsonValue)
            = if (value.inside is java.lang.Boolean|| value.inside is Boolean) value.boolean else null

    override fun toJson(value: Any): String {
        return value.toString()
    }

}

class _StringConverter: Converter2 {
//    override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//        return field?.returnType == String::class.java || value::class.java == String::class.java
//    }

    override fun fromJson(field: KProperty<*>?, value: JsonValue): String? {
        return if (value.inside is String || value.inside is java.lang.String) value.string else null
    }

    override fun toJson(value: Any): String {
        return "\"" + value.toString() + "\""
    }

}

class _ArrayConverter(val parent: JsonConverter): Converter2 {
//    override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//        return value is Collection<*>
//    }

    override fun fromJson(prop: KProperty<*>?, value: JsonValue): List<Any?>? {
        if (! (value.inside is Collection<*>)) return null

        val propType =
            if (prop != null) {
                if (! (prop.getter.returnType.javaType is ParameterizedType)) {
                    println("PROBLEM")
                }
                (prop.getter.returnType.javaType as ParameterizedType).rawType
            } else {
                value.inside.javaClass
            }
//        val propType = (prop?.getter?.returnType?.javaType as ParameterizedType)?.rawType
//                ?: value.inside.javaClass
        val result = arrayListOf<Any?>()
        if (propType == java.util.List::class.java && prop != null) {
            val elementType = prop.returnType.arguments[0].type
            val jValue = value.array
            if (jValue != null) {
                jValue.forEach {
                    if (it is JsonObject) {
                        val cls = elementType!!.javaType as Class<*>
                        val inflated = parent.fromJsonObject(it, cls, elementType.jvmErasure)
                        result.add(inflated)
                    } else {
                        result.add(it)
                    }
                }
            }
        }
        return if (! result.isEmpty()) result else null
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

class _ObjectConverter(val parent: JsonConverter): Converter2 {
//    override fun canConvert(field: KProperty<*>?, value: Any): Boolean {
//        return true
//    }

    override fun fromJson(field: KProperty<*>?, value: JsonValue): Any? {
        if (field == null) return null
        else {
            val fieldType = field?.returnType!!::class.java ?: value.type::class.java
            val obj = value.obj
            return null
//            return if (obj != null) parent.fromJsonObject(obj, fieldType, null)
//            else null
        }
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

//    val converters = arrayOf(
//            _BooleanConverter(),
//            _IntConverter(),
//            _StringConverter()
//            ,
//            _ArrayConverter(this),
//            _ObjectConverter(this)
//    )
//
//    fun toJson(value: Any) : String {
//        val c = converters.firstOrNull() { it.canConvert(null, "foo") }
//        val converter = converters.firstOrNull() { it.canConvert(null, value) }
//        val result =
//            if (converter != null) {
//                val json = converter.toJson(value)
//                json
//            } else {
//                throw KlaxonException("Couldn't find a converter for $value")
//            }
//        return result
//    }
}