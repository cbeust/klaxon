package com.beust.klaxon

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Field
import kotlin.reflect.full.declaredMemberProperties

fun matchType(field: Field?, value: JsonValue, type: Class<*>) : Boolean {
    val thisType = field?.type ?: value.inside.javaClass
    return thisType == type
}

class IntConverter : TypeConverter<Int> {
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Integer::class.java)) value.int else null
    override fun toJson(obj: Int) = obj.toString()
}

class LongConverter : TypeConverter<Int> {
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Long::class.java)) value.int else null
    override fun toJson(obj: Int) = obj.toString()
}

class StringConverter : TypeConverter<String> {
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.String::class.java)) value.string else null
    override fun toJson(obj: String) = "\"$obj\""
}

class BooleanConverter : TypeConverter<Boolean> {
    override fun fromJson(field: Field?, value: JsonValue)
            = if (matchType(field, value, java.lang.Boolean::class.java)) value.boolean else null
    override fun toJson(obj: Boolean) = obj.toString()
}

class ObjectConverter(private val jsonObjectConverter: JsonObjectConverter) : TypeConverter<Any?> {
    override fun fromJson(field: Field?, value: JsonValue): Any? {
        val fieldType = field?.type ?: value.type
        val obj = value.obj
        return if (obj != null) jsonObjectConverter.fromJsonObject(obj, fieldType)
            else null
    }

    override fun toJson(obj: Any?): String {
        val sb = StringBuilder("{")

        if (obj != null) {
            var first = true
            obj::class.declaredMemberProperties . forEach { property ->
                val p = property.getter.call(obj)
                if (! first) sb.append(", ")
                first = false
                sb.append("\"" + property.name + "\" : ")
                sb.append(jsonObjectConverter.toJsonString(p))
            }
        } else {
            sb.append("<null object>")
        }

        sb.append("}")
        return sb.toString()
    }

}
class ArrayConverter(private val jsonObjectConverter: JsonObjectConverter) : TypeConverter<List<Any?>> {
    override fun fromJson(field: Field?, value: JsonValue): List<Any?>? {
        val fieldType = field?.type ?: value.type
        val genericType = field?.genericType ?: value.type
        val result = arrayListOf<Any?>()
        if (fieldType.isAssignableFrom(List::class.java)) {
            val elementType = (genericType as ParameterizedTypeImpl).actualTypeArguments[0]
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

    override fun toJson(obj: List<Any?>): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
