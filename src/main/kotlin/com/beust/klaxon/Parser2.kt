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

class Pair(status: Status, tokenType: Type)

trait Processor {
    fun process(): World
}

class DefaultProcessor : Processor {
    override fun process(): World {
        throw RuntimeException("ERROR")
    }
}
class StateMachine {
    val map = hashMapOf<Pair, (world: World, token: Token) -> World>()

    fun put(status: Status, tokenType: Type, processor: (world: World, token: Token) -> World) {
        map.put(Pair(status, tokenType), processor)
    }

    fun next(world: World, token: Token) : World {
        val processor = map.get(Pair(world.status, token.tokenType))
        return if (processor != null) processor(world, token)
        else throw RuntimeException("No state found: ${token}")
    }
}

public class Parser2 {
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

        sm.put(Status.IN_FINISHED_VALUE, Type.END, { (world: World, token: Token) ->
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
                world.pushStatus(world.statusStack.get(0))
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
            world.parent.put(key!!, token.value!!)
            world.status = world.statusStack.get(0)
            world
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { (world: World, token: Token) ->
            world.popStatus()
            val key = world.popValue()as JsonString
            world.parent = world.valueStack.getFirst()
            val newArray = JsonArray()
            world.parent.put(key!!, newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        sm.put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { (world: World, token: Token) ->
            world.popStatus()
            val key = world.popValue() as JsonString
            world.parent = world.valueStack.getFirst()
            val newObject = JsonObject()
            world.parent.put(key!!, newObject)
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
        sm.put(Status.IN_ARRAY, Type.VALUE, { (world: World, token: Token) ->
            if (world.valueStack.size() > 1) {
                world.popStatus()
                world.popValue()
                world.status = world.statusStack.get(0) // peek
            } else {
                world.status = Status.IN_FINISHED_VALUE
            }
            world
        })
        sm.put(Status.IN_ARRAY, Type.VALUE, { (world: World, token: Token) ->
            val value = world.valueStack.getFirst() as JsonArray
            val newObject = JsonObject()
            value.add(newObject)
            world.pushAndSet(Status.IN_OBJECT, newObject)
        })
        sm.put(Status.IN_ARRAY, Type.VALUE, { (world: World, token: Token) ->
            val value = world.valueStack.getFirst() as JsonArray
            val newArray = JsonArray()
            value.add(newArray)
            world.pushAndSet(Status.IN_ARRAY, newArray)
        })
        // else error

        val lexer = Lexer(inputStream)
        var token = lexer.nextToken()
        var tokenType = token.tokenType

        var world = World(Status.INIT)
        while (world.status != Status.END) {
            world = sm.next(world, token)
            token = lexer.nextToken()
            tokenType = token.tokenType
        }

        return world.result
    }
}