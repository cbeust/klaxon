package com.beust.klaxon

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Field

class ObjectAdapter(private val jsonObjectConverter: JsonObjectConverter) : TypeConverter<Any?> {
    override fun fromJson(field: Field, value: JsonValue): Any? {
        val fieldType = field.type
        val obj = value.obj
        return if (obj != null) jsonObjectConverter.fromJsonObject(obj, fieldType)
            else null
    }

    override fun toJson(o: Any?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
class ArrayAdapter(private val jsonObjectConverter: JsonObjectConverter) : TypeConverter<List<Any?>> {
    override fun fromJson(field: Field, value: JsonValue): List<Any?>? {
        val fieldType = field.type
        val result = arrayListOf<Any?>()
        if (fieldType.isAssignableFrom(List::class.java)) {
            val elementType = (field.genericType as ParameterizedTypeImpl).actualTypeArguments[0]
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

    override fun toJson(o: List<Any?>): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class IntAdapter : TypeConverter<Int> {
    override fun fromJson(field: Field, value: JsonValue)
            = if (field.type == java.lang.Integer::class.java) value.int else null
    override fun toJson(o: Int) = o.toString() ?: "Illegal Int value"
}

class LongAdapter : TypeConverter<Int> {
    override fun fromJson(field: Field, value: JsonValue)
            = if (field.type == java.lang.Long::class.java) value.int else null
    override fun toJson(o: Int) = o.toString() ?: "Illegal Int value"
}

class StringAdapter : TypeConverter<String> {
    override fun fromJson(field: Field, value: JsonValue)
            = if (field.type == java.lang.String::class.java) value.string else null
    override fun toJson(o: String) = o.toString() ?: "Illegal String value"
}

class BooleanAdapter : TypeConverter<Boolean> {
    override fun fromJson(field: Field, value: JsonValue)
            = if (field.type == java.lang.Boolean::class.java) value.boolean else null
    override fun toJson(o: Boolean) = o.toString() ?: "Illegal Boolean value"
}
