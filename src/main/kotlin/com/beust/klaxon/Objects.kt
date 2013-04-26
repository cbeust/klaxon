package com.beust.klaxon

import java.util.ArrayList
import java.util.HashMap

data open public class JsonObject(val map: MutableMap<String, Any>
                             = HashMap<String, Any>())
        : MutableMap<String, Any> by map {

    override fun put(key: String, value: Any) : JsonObject {
        map.put(key, value)
        return this
    }

    open fun getArray() : ArrayList<JsonObject>? {
        return null
    }

    open fun obj(id: String) : JsonObject? {
        return map.get(id) as JsonObject
    }

    open fun array(id: String) : JsonArray? {
        return map.get(id) as JsonArray
    }

    open fun long(id: String) : Long? {
        return map.get(id) as Long?
    }

    open fun string(id: String) : String? {
        return map.get(id) as String
    }

    open fun double(id: String) : Double? {
        return map.get(id) as Double
    }

    open fun boolean(id: String) : Boolean? {
        return map.get(id) as Boolean
    }

    open fun asString() : String {
        throw RuntimeException("Not a String")
    }

    open fun equals(other : Any?) : Boolean {
        return (other as JsonObject).map.equals(map)
    }

    open fun hashCode() : Int {
        return map.hashCode()
    }
}

data public class JsonArray(val value : MutableList<Any> = ArrayList<Any>()) {
    fun add(a: Any) : JsonArray {
        value.add(a)
        return this
    }

    fun find(predicate: (Any) -> Boolean) : Any? {
        return value.find(predicate)
    }

    fun filter(predicate: (Any) -> Boolean) : Any? {
        return value.filter(predicate)
    }
//{
//
//    override fun isEmpty() : Boolean {
//        return value.isEmpty()
//    }
//
//    open fun equals(other : Any?) : Boolean {
//        return (other as JsonArray).value.equals(value)
//    }
//
//    open fun hashCode() : Int {
//        return value.hashCode()
//    }
//
//    override fun containsAll(c: Collection<Any?>) : Boolean {
//        return value.containsAll(c)
//    }
//
//    override fun <T> toArray(a: Array<out T>) : Array<T> {
//        return value.toArray(a)
//    }
//
//    override fun iterator() : Iterator<Any> {
//        return value.iterator()
//    }
}