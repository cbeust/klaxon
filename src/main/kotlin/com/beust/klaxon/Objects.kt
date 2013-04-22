package com.beust.klaxon

open public class JsonObject {
    val map = hashMapOf<String, JsonObject>()

    fun error() : String {
        throw RuntimeException("Can't convert ${map} to string")
    }

    fun put(key: String, value: JsonObject) {
        map.put(key, value)
    }

    open fun toString() : String {
        return map.toString()
    }

    open fun asString() : String {
        return error()
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
    override fun toString() : String {
        return "{Long: $value}"
    }
}

public class JsonDouble(val value: Double): JsonObject() {
    override fun toString() : String {
        return "{Double: $value}"
    }
}

public class JsonBoolean(val value: Boolean) : JsonObject() {
    override fun toString() : String {
        return "{Boolean: $value}"
    }
}

public class JsonArray() : JsonObject() {
    val array = arrayListOf<JsonObject>()

    fun add(o : JsonObject) {
        array.add(o)
    }

    override fun toString() : String {
        val result = "{Array: " + array.toString() + "}"
        return result
    }
}