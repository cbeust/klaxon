package com.beust.klaxon

import java.util.*

class World(var status : Status) {
    private val statusStack = LinkedList<Status>()
    private val valueStack = LinkedList<Any>()
    var result : Any? = null
    var parent = JsonObject()

    fun pushAndSet(status: Status, value: Any) : World {
        pushStatus(status)
        pushValue(value)
        this.status = status
        return this
    }

    private fun pushStatus(status: Status) : World {
        statusStack.addFirst(status)
        return this
    }

    private fun pushValue(value: Any) : World {
        valueStack.addFirst(value)
        return this
    }

    /**
     * The index of the character we are currently on.
     */
    var index: Int = 0

    /**
     * The current line.
     */
    var line: Int = 0

    fun popValue() : Any {
        return valueStack.removeFirst()
    }

    fun popStatus() : Status {
        return statusStack.removeFirst()
    }

    fun getFirstObject() : JsonObject {
        return valueStack.first as JsonObject
    }

    @Suppress("UNCHECKED_CAST")
    fun getFirstArray() : JsonArray<Any?> {
        return valueStack.first as JsonArray<Any?>
    }

    fun peekStatus() : Status {
        return statusStack[0]
    }

    fun hasValues() : Boolean {
        return valueStack.size > 1
    }

}