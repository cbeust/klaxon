package com.beust.klaxon

import java.io.InputStream
import java.util.LinkedList

class World(var status : Status) {
    val statusStack = LinkedList<Status>()
    val valueStack = LinkedList<JsonObject>()
    var result = JsonObject()
    var parent = JsonObject()

    fun pushAndSet(status: Status, value: JsonObject) : World {
        pushStatus(status)
        pushValue(value)
        this.status = status
        return this
    }

    fun pushStatus(status: Status) : World {
        statusStack.addFirst(status)
        return this
    }

    fun pushValue(value: JsonObject) : World {
        valueStack.addFirst(value)
        return this
    }

    fun popValue() : JsonObject {
        return valueStack.removeFirst()
    }

    fun popStatus() : Status {
        return statusStack.removeFirst()
    }
}

data class Pair(val status: Status, val tokenType: Type)

class StateMachine {
    val map = hashMapOf<Pair, (world: World, token: Token) -> World>()

    fun put(status: Status, tokenType: Type, processor: (world: World, token: Token) -> World) {
        map.put(Pair(status, tokenType), processor)
    }

    fun next(world: World, token: Token) : World {
        val pair = Pair(world.status, token.tokenType)
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

public class Parser2 {
    val verbose = false

    fun log(s: String) {
        if (verbose) {
            println("[Parser2] ${s}")
        }
    }
    public fun parse(inputStream : InputStream) : JsonObject {

        val sm = StateMachine()

        sm.put(Status.INIT, Type.VALUE, { (world: World, token: Token) ->
            world.pushAndSet(Status.IN_FINISHED_VALUE, token.value!!)
        })
        sm.put(Status.INIT, Type.LEFT_BRACE, { (world: World, token: Token) ->
            world.pushAndSet(Status.IN_OBJECT, JsonObject())
        })
        sm.put(Status.INIT, Type.LEFT_BRACKET, { (world: World, token: Token) ->
            world.pushAndSet(Status.IN_ARRAY, JsonArray())
        })
        // else error

        sm.put(Status.IN_FINISHED_VALUE, Type.EOF, { (world: World, token: Token) ->
            world.result = world.popValue()
            world
        })
        // else error


        sm.put(Status.IN_OBJECT, Type.COMMA, { (world: World, token: Token) ->
            world
        })
        sm.put(Status.IN_OBJECT, Type.VALUE, { (world: World, token: Token) ->
            world.pushAndSet(Status.PASSED_PAIR_KEY, token.value!!)
        })
        sm.put(Status.IN_OBJECT, Type.RIGHT_BRACE, { (world: World, token: Token) ->
            if (world.valueStack.size() > 1) {
                world.popStatus()
                world.popValue()
                world.status = world.statusStack.get(0)
            } else {
                world.status = Status.IN_FINISHED_VALUE
            }
            world
        })


        sm.put(Status.PASSED_PAIR_KEY, Type.COLON, { (world: World, token: Token) ->
            world
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.VALUE, { (world: World, token: Token) ->
            world.popStatus()
            val key = world.popValue() as JsonString
            world.parent = world.valueStack.getFirst()
            world.parent.put(key, token.value!!)
            world.status = world.statusStack.get(0)
            world
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { (world: World, token: Token) ->
            world.popStatus()
            val key = world.popValue()as JsonString
            world.parent = world.valueStack.getFirst()
            val newArray = JsonArray()
            world.parent.put(key, newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACE, { (world: World, token: Token) ->
            world.popStatus()
            val key = world.popValue() as JsonString
            world.parent = world.valueStack.getFirst()
            val newObject = JsonObject()
            world.parent.put(key, newObject)
            world.pushAndSet(Status.IN_OBJECT, newObject)
        })
        // else error

        sm.put(Status.IN_ARRAY, Type.COMMA, { (world: World, token: Token) ->
            world
        })
        sm.put(Status.IN_ARRAY, Type.VALUE, { (world: World, token: Token) ->
            val value = world.valueStack.getFirst() as JsonArray
            value.add(token.value!!)
            world
        })
        sm.put(Status.IN_ARRAY, Type.RIGHT_BRACKET, { (world: World, token: Token) ->
            if (world.valueStack.size() > 1) {
                world.popStatus()
                world.popValue()
                world.status = world.statusStack.get(0) // peek
            } else {
                world.status = Status.IN_FINISHED_VALUE
            }
            world
        })
        sm.put(Status.IN_ARRAY, Type.LEFT_BRACE, { (world: World, token: Token) ->
            val value = world.valueStack.getFirst() as JsonArray
            val newObject = JsonObject()
            value.add(newObject)
            world.pushAndSet(Status.IN_OBJECT, newObject)
        })
        sm.put(Status.IN_ARRAY, Type.LEFT_BRACKET, { (world: World, token: Token) ->
            val value = world.valueStack.getFirst() as JsonArray
            val newArray = JsonArray()
            value.add(newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        // else error

        val lexer = Lexer(inputStream)

        var world = World(Status.INIT)
        do {
            var token = lexer.nextToken()
            log("Token: ${token}")
            world = sm.next(world, token)
        } while (token.tokenType != Type.EOF)

        return world.result
    }
}