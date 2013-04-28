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

    open fun <T> array(thisType: T, id: String) : JsonArray<T>? {
        return map.get(id) as JsonArray<T>
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

// Because of http://youtrack.jetbrains.com/issue/KT-3546, I need to do some
// manual delegation here
data public class JsonArray<T>(val value : MutableList<T> = ArrayList<T>()) {
    fun add(a: T) : JsonArray<T> {
        value.add(a)
        value.forEach {  }
        return this
    }

    public fun filter(predicate: (T) -> Boolean) : List<T> {
        return value?.filter(predicate)
    }

    public fun <R> flatMap(transform: (T)-> Iterable<R>) : List<R> {
        return value?.flatMap(transform)
    }

    open fun string(id: String) : JsonArray<String> {
        var result = JsonArray<String>()
        value.forEach {
            val obj = (it as JsonObject).string(id)
            result.add(obj!!)
        }
        return result
    }

    open fun obj(id: String) : JsonArray<JsonObject> {
        var result = JsonArray<JsonObject>()
        value.forEach {
            val obj = (it as JsonObject).obj(id)
            result.add(obj!!)
        }
        return result
    }

//    open fun arrayObj(id: String) : JsonArray<JsonObject> {
//        var result = JsonArray<JsonObject>()
//        value.forEach {
//            val o = it as JsonObject
//            val obj = o.array(id)
//            result.addAll(obj!!)
//        }
//        return result
//    }

    public fun forEach(field: String, operation: (JsonObject) -> Unit) : Unit {
        for (element in value) operation(element as JsonObject)
    }

    fun find(predicate: (T) -> Boolean) : T? {
        return value.find(predicate)
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