package com.beust.klaxon

import java.io.*
import java.nio.charset.Charset
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

    fun pushStatus(status: Status) : World {
        statusStack.addFirst(status)
        return this
    }

    fun pushValue(value: Any) : World {
        valueStack.addFirst(value)
        return this
    }

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
        return statusStack.get(0)
    }

    fun hasValues() : Boolean {
        return valueStack.size > 1
    }
}

private data class TokenStatus(val status: Status, val tokenType: Type)

class StateMachine {
    private val map = hashMapOf<TokenStatus, (world: World, token: Token) -> World>()

    fun put(status: Status, tokenType: Type, processor: (world: World, token: Token) -> World) {
        map.put(TokenStatus(status, tokenType), processor)
    }

    fun next(world: World, token: Token) : World {
        val pair = TokenStatus(world.status, token.tokenType)
        val processor = map.get(pair)
        val result = if (processor != null) {
            processor(world, token)
        } else {
            val message = "No state found: ${world.status} ${token}"
            throw RuntimeException(message)
        }

//        println("${status} ${token.tokenType} -> ${world.status}")
        return result
    }
}

/**
 * Main entry for Klaxon's parser.
 */
class Parser {
    val verbose = false

    fun log(s: String) {
        if (verbose) {
            println("[Parser2] ${s}")
        }
    }

    fun parse(rawValue: StringBuilder): Any? =
        StringReader(rawValue.toString()).use {
            parse(it)
        }

    fun parse(fileName: String) : Any? =
        FileInputStream(File(fileName)).use {
            parse(it)
        }

    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8): Any? {
        return parse(inputStream.reader(charset))
    }

    fun parse(reader: Reader): Any? {

        val sm = StateMachine()

        sm.put(Status.INIT, Type.VALUE, { world: World, token: Token ->
            world.pushAndSet(Status.IN_FINISHED_VALUE, token.value!!)
        })
        sm.put(Status.INIT, Type.LEFT_BRACE, { world: World, token: Token ->
            world.pushAndSet(Status.IN_OBJECT, JsonObject())
        })
        sm.put(Status.INIT, Type.LEFT_BRACKET, { world: World, token: Token ->
            world.pushAndSet(Status.IN_ARRAY, JsonArray<Any>())
        })
        // else error

        sm.put(Status.IN_FINISHED_VALUE, Type.EOF, { world: World, token: Token ->
            world.result = world.popValue()
            world
        })
        // else error


        sm.put(Status.IN_OBJECT, Type.COMMA, { world: World, token: Token ->
            world
        })
        sm.put(Status.IN_OBJECT, Type.VALUE, { world: World, token: Token ->
            world.pushAndSet(Status.PASSED_PAIR_KEY, token.value!!)
        })
        sm.put(Status.IN_OBJECT, Type.RIGHT_BRACE, { world: World, token: Token ->
            if (world.hasValues()) {
                world.popStatus()
                world.popValue()
                world.status = world.peekStatus()
            } else {
                world.status = Status.IN_FINISHED_VALUE
            }
            world
        })


        sm.put(Status.PASSED_PAIR_KEY, Type.COLON, { world: World, token: Token ->
            world
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.VALUE, { world: World, token: Token ->
            world.popStatus()
            val key = world.popValue() as String
            world.parent = world.getFirstObject()
            world.parent.put(key, token.value)
            world.status = world.peekStatus()
            world
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { world: World, token: Token ->
            world.popStatus()
            val key = world.popValue() as String
            world.parent = world.getFirstObject()
            val newArray = JsonArray<Any>()
            world.parent.put(key, newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACE, { world: World, token: Token ->
            world.popStatus()
            val key = world.popValue() as String
            world.parent = world.getFirstObject()
            val newObject = JsonObject()
            world.parent.put(key, newObject)
            world.pushAndSet(Status.IN_OBJECT, newObject)
        })
        // else error

        sm.put(Status.IN_ARRAY, Type.COMMA, { world: World, token: Token ->
            world
        })
        sm.put(Status.IN_ARRAY, Type.VALUE, { world: World, token: Token ->
            val value = world.getFirstArray()
            value.add(token.value)
            world
        })
        sm.put(Status.IN_ARRAY, Type.RIGHT_BRACKET, { world: World, token: Token ->
            if (world.hasValues()) {
                world.popStatus()
                world.popValue()
                world.status = world.peekStatus()
            } else {
                world.status = Status.IN_FINISHED_VALUE
            }
            world
        })
        sm.put(Status.IN_ARRAY, Type.LEFT_BRACE, { world: World, token: Token ->
            val value = world.getFirstArray()
            val newObject = JsonObject()
            value.add(newObject)
            world.pushAndSet(Status.IN_OBJECT, newObject)
        })
        sm.put(Status.IN_ARRAY, Type.LEFT_BRACKET, { world: World, token: Token ->
            val value = world.getFirstArray()
            val newArray = JsonArray<Any>()
            value.add(newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        // else error

        val lexer = Lexer(reader)

        var world = World(Status.INIT)
        do {
            val token = lexer.nextToken()
            log("Token: ${token}")
            world = sm.next(world, token)
        } while (token.tokenType != Type.EOF)

        return world.result
    }
}