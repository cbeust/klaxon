package com.beust.klaxon

import java.util.Arrays

open public class JsonObject {
    val map = hashMapOf<JsonString, JsonObject>()

    fun put(key: JsonString, value: JsonObject) : JsonObject {
        map.put(key, value)
        return this
    }

    open fun toString() : String {
        return map.toString()
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

    fun get(key : String) : JsonObject? {
        return map.get(key)
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
        return "{String: \"$value\"}"
    }
}

data public class JsonLong(val value: Long) : JsonObject() {
    override fun asLong() : Long {
        return value
    }

    override fun toString() : String {
        return "{Long: $value}"
    }
}

data public class JsonDouble(val value: Double): JsonObject() {
    override fun asDouble() : Double {
        return value
    }

    override fun toString() : String {
        return "{Double: $value}"
    }

}

data public class JsonBoolean(val value: Boolean) : JsonObject() {
    override fun asBoolean() : Boolean {
        return value
    }

    override fun toString() : String {
        return "{Boolean: $value}"
    }
}

public class JsonArray() : JsonObject() {
    val value = arrayListOf<JsonObject>()

    fun add(o : JsonObject) : JsonArray {
        value.add(o)
        return this
    }

    override fun asList() : List<JsonObject> {
        return value
    }

    override fun toString() : String {
        val result = "{Array: " + value.toString() + "}"
        return result
    }

    override fun equals(other : Any?) : Boolean {
        return (other as JsonArray).value.equals(value)
    }

    override fun hashCode() : Int {
        return value.hashCode()
    }
}