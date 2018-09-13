package com.beust.klaxon

import java.util.*

class World(var status : Status, val pathMatchers: List<PathMatcher> = emptyList()) {
    private val statusStack = LinkedList<Status>()
    private val valueStack = LinkedList<Any>()
    var result : Any? = null
    var parent = JsonObject()

    /**
     * The path of the current JSON element.
     * See https://github.com/json-path/JsonPath
     */
    val path: String get() {
        val result = arrayListOf("$")
        valueStack.reversed().forEach { value ->
            when(value) {
                is JsonObject -> {
                    if (value.any()) {
                        result.add("." + value.keys.last().toString())
                    }
                }
                is JsonArray<*> -> {
                    result.add("[" + (value.size - 1) + "]")
                }
                else -> {
                    result.add("." + value)
                }
            }
        }
        return result.joinToString("")
    }

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

    fun isNestedStatus() : Boolean {
        return statusStack.size > 1
    }

    fun hasValues() : Boolean {
        return valueStack.size > 1
    }

    internal fun foundValue() {
        val first = valueStack.peekFirst()
        if (first is JsonObject && first.isNotEmpty()) {
            val value = first.values.last()
            if (value != null && value !is JsonArray<*> && value !is JsonObject) {
                pathMatchers.filter {
                    it.pathMatches(path)
                }.forEach {
                    it.onMatch(path, value)
                }
            }
        }
    }

}