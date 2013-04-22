package com.beust.klaxon

open public class JsonObject {
    val map = hashMapOf<String, JsonObject>()

    fun put(key: String, value: JsonObject) {
        map.put(key, value)
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
}

public class JsonString(val value: String) : JsonObject() {
    override fun asString() : String {
        return value
    }

    override fun toString() : String {
        return "{String: \"$value\"}"
    }
}

public class JsonLong(val value: Long) : JsonObject() {
    override fun asLong() : Long {
        return value
    }

    override fun toString() : String {
        return "{Long: $value}"
    }
}

public class JsonDouble(val value: Double): JsonObject() {
    override open fun asDouble() : Double {
        return value
    }

    override fun toString() : String {
        return "{Double: $value}"
    }
}

public class JsonBoolean(val value: Boolean) : JsonObject() {
    override open fun asBoolean() : Boolean {
        return value
    }

    override fun toString() : String {
        return "{Boolean: $value}"
    }
}

public class JsonArray() : JsonObject() {
    val array = arrayListOf<JsonObject>()

    fun add(o : JsonObject) {
        array.add(o)
    }

    override fun asList() : List<JsonObject> {
        return array
    }

    override fun toString() : String {
        val result = "{Array: " + array.toString() + "}"
        return result
    }
}