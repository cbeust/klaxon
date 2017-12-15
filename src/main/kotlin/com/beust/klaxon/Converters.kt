package com.beust.klaxon

import java.lang.reflect.Field
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

fun matchType(field: Field?, value: JsonValue, type: Class<*>) : Boolean {
    val thisType = field?.type ?: value.inside.javaClass
    return thisType.isAssignableFrom(type)
}

class IntConverter : Converter2<Int> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == Int::class.java || value::class == Int::class.java
                || field?.type == Integer::class.java || value::class == Integer::class
    }
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Integer::class.java)) value.int else null
    override fun toJson(obj: Any) = obj.toString()
}

//class LongConverter : TypeConverter<Int> {
//    override fun fromJson(field: Field?, value: JsonValue)
//            = if (matchType(field, value, java.lang.Long::class.java)) value.int else null
//    override fun toJson(obj: Int) = obj.toString()
//}

class StringConverter : Converter2<String> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == String::class.java || value::class.java == String::class.java
    }
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.String::class.java)) value.string else null
    override fun toJson(obj: Any) = "\"$obj\""
}

class BooleanConverter : Converter2<Boolean> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return field?.type == Boolean::class.java || value::class == Boolean::class.java
                || field?.type == Boolean::class || value::class == Boolean::class
    }
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Boolean::class.java)) value.boolean else null
    override fun toJson(obj: Any) = obj.toString()
}

class ObjectConverter(private val jsonObjectConverter: JsonObjectConverter) : Converter2<Any?> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return true
    }

    override fun fromJson(field: Field?, value: JsonValue): Any? {
        val fieldType = field?.type ?: value.type
        val obj = value.obj
        return if (obj != null) jsonObjectConverter.fromJsonObject(obj, fieldType)
            else null
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
                    sb.append(jsonObjectConverter.toJsonString(p!!))
                }
        return "{ " + sb.toString() + "}"
    }

}
class ArrayConverter(private val jsonObjectConverter: JsonObjectConverter) : Converter2<List<Any?>> {
    override fun canConvert(field: Field?, value: Any): Boolean {
        return value is Collection<*>
    }

    override fun fromJson(field: Field?, value: JsonValue): List<Any?>? {
        val fieldType = field?.type ?: value.type
        val result = arrayListOf<Any?>()
        if (fieldType.isAssignableFrom(List::class.java)) {
            val elementType = value.genericType
            val jValue = value.array
            if (jValue != null) {
                jValue.forEach {
                    if (it is JsonObject) {
                        val inflated = jsonObjectConverter.fromJsonObject(it, elementType as Class<*>)
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
                result.append(jsonObjectConverter.toJsonString(it))
            }
        }
        return "[" + result.toString() + "]"
    }

}
