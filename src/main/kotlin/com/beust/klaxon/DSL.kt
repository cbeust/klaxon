package com.beust.klaxon

import java.util.ArrayList
import java.util.HashMap

public trait JsonBase {
    fun valueToString(v: Any) : String {
        val result = StringBuilder()

        when (v) {
            is JsonObject -> result.append(v.toJsonString())
            is JsonArray<*> -> result.append(v.toJsonString())
            is String -> result.append("\"").append(v).append("\"")
            else -> result.append(v)
        }

        return result.toString()
    }
}

public data class JsonObject(val map: MutableMap<String, Any>
                             = HashMap<String, Any>())
        : JsonBase, MutableMap<String, Any> by map {

    override fun put(key: String, value: Any) : JsonObject {
        map.put(key, value)
        return this
    }

    fun obj(fieldName: String) : JsonObject? {
        return map.get(fieldName) as JsonObject
    }

    [suppress("UNCHECKED_CAST")]
    fun <T> array(fieldName: String) : JsonArray<T>? {
        return map.get(fieldName) as JsonArray<T>
    }

    fun long(fieldName: String) : Long? {
        return map.get(fieldName) as Long?
    }

    fun string(fieldName: String) : String? {
        return map.get(fieldName) as String
    }

    fun double(fieldName: String) : Double? {
        return map.get(fieldName) as Double
    }

    fun boolean(fieldName: String) : Boolean? {
        return map.get(fieldName) as Boolean
    }

    fun asString() : String {
        throw RuntimeException("Not a String")
    }

    fun toJsonString() : String {
        val result = StringBuilder();
        result.append("{ ")
        var comma = false
        for ((k, v) in map) {
            if (comma) {
                result.append(", ")
            } else {
                comma = true
            }
            result.append("\"").append(k).append("\" : ")
            result.append(valueToString(v)).append(" ")
        }
        result.append("} ")

        return result.toString()
    }
}

public data class JsonArray<T>(val value : MutableList<T> = ArrayList<T>()) : JsonBase, List<T> by value {

    fun add(a: T) : JsonArray<T> {
        value.add(a)
        value.forEach {  }
        return this
    }

    fun string(id: String) : JsonArray<String>? {
        var result = JsonArray<String>()
        value.forEach {
            val obj = (it as JsonObject).string(id)
            result.add(obj!!)
        }
        return result
    }

    fun obj(id: String) : JsonArray<JsonObject> {
        var result = JsonArray<JsonObject>()
        value.forEach {
            val obj = (it as JsonObject).obj(id)
            result.add(obj!!)
        }
        return result
    }

    fun long(id: String) : JsonArray<Long> {
        var result = JsonArray<Long>()
        value.forEach {
            val obj = (it as JsonObject).long(id)
            result.add(obj!!)
        }
        return result
    }

    fun toJsonString() : String {
        val result = StringBuilder();
        result.append("[ ")
        var comma = false
        value.forEach {
            if (comma) {
                result.append(", ")
            } else {
                comma = true
            }
            result.append(valueToString(it)).append(" ")
        }
        result.append("] ")

        return result.toString()
    }

}