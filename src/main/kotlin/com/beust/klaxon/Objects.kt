package com.beust.klaxon

import java.util.ArrayList
import java.util.HashMap

open public class JsonObject(val map: MutableMap<String, JsonObject>
                             = HashMap<String, JsonObject>())
        : Map<String, JsonObject> by map {
    fun put(key: String, value: String) : JsonObject {
        return put(key, JsonString(value))
    }

    fun put(key: String, value: Long) : JsonObject {
        return put(key, JsonLong(value))
    }

    fun put(key: String, value: Double) : JsonObject {
        return put(key, JsonDouble(value))
    }

    fun put(key: String, value: JsonObject) : JsonObject {
        map.put(key, value)
        return this
    }

    open fun toString() : String {
        return map.toString()
    }

    open fun getArray() : ArrayList<JsonObject>? {
        return null
    }

    open fun asArray() : ArrayList<JsonObject>{
        throw RuntimeException("Not an array")
    }

    open fun asString() : String {
        throw RuntimeException("Not a String")
    }

    open fun asBoolean() : Boolean {
        throw RuntimeException("Not a Boolean")
    }

    open fun asDouble() : Double {
        throw RuntimeException("Not a Double")
    }

    open fun asLong() : Long {
        throw RuntimeException("Not a Long")
    }

    open fun asList() : List<JsonObject> {
        throw RuntimeException("Not an array")
    }

    open fun equals(other : Any?) : Boolean {
        return (other as JsonObject).map.equals(map)
    }

    open fun hashCode() : Int {
        return map.hashCode()
    }
}

data public class JsonString(val value: String) : JsonObject() {
    override fun asString() : String {
        return value
    }

    override fun toString() : String {
        return "\"${value}\""
    }
}

data public class JsonLong(val value: Long) : JsonObject() {
    override fun asLong() : Long {
        return value
    }

    override fun toString() : String {
        return "${value}"
    }
}

data public class JsonDouble(val value: Double): JsonObject() {
    override fun asDouble() : Double {
        return value
    }

    override fun toString() : String {
        return "${value}"
    }

}

data public class JsonBoolean(val value: Boolean) : JsonObject() {
    override fun asBoolean() : Boolean {
        return value
    }

    override fun toString() : String {
        return "${value}"
    }
}

public class JsonArray(val value : ArrayList<JsonObject> = ArrayList<JsonObject>())
        : JsonObject(), Collection<JsonObject> by value {

    override fun getArray() : ArrayList<JsonObject>? {
        return asArray()
    }

    override fun size() : Int {
        return value.size()
    }

    override fun isEmpty() : Boolean {
        return value.isEmpty()
    }

    override fun asArray() : ArrayList<JsonObject>{
        return value
    }

    //    override fun get(key: String, filter: (JsonObject?) -> Boolean) : JsonObject? {
//        value.filter {  }
//        val result = JsonArray()
//        value.forEach {
//            val jo = it.get(key)
//            if (filter(jo)) {
//                result.add(jo!!)
//            }
//        }
//        return result
//    }

    fun add(value: Long) : JsonArray {
        return add(JsonLong(value))
    }

    fun add(value: String) : JsonArray {
        return add(JsonString(value))
    }

    fun add(value: Double) : JsonArray {
        return add(JsonDouble(value))
    }

    fun add(value: Boolean) : JsonArray {
        return add(JsonBoolean(value))
    }

    fun add(o : JsonObject) : JsonArray {
        value.add(o)
        return this
    }

    override fun asList() : List<JsonObject> {
        return value
    }

    override fun toString() : String {
        val result = value.toString()
        return result
    }

    override fun equals(other : Any?) : Boolean {
        return (other as JsonArray).value.equals(value)
    }

    override fun hashCode() : Int {
        return value.hashCode()
    }
}