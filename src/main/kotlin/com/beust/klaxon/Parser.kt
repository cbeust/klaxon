package com.beust.klaxon

import java.io.*
import java.nio.charset.Charset

/**
 * Main entry for Klaxon's parser.
 */
class Parser {
    private val verbose = false

    private fun log(s: String) {
        if (Debug.verbose) {
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
        with(sm) {
            put(Status.INIT, Type.VALUE, { world: World, token: Token ->
                world.pushAndSet(Status.IN_FINISHED_VALUE, token.value!!)
            })
            put(Status.INIT, Type.LEFT_BRACE, { world: World, _: Token ->
                world.pushAndSet(Status.IN_OBJECT, JsonObject())
            })
            put(Status.INIT, Type.LEFT_BRACKET, { world: World, _: Token ->
                world.pushAndSet(Status.IN_ARRAY, JsonArray<Any>())
            })
            // else error
    
            put(Status.IN_FINISHED_VALUE, Type.EOF, { world: World, _: Token ->
                world.result = world.popValue()
                world
            })
            // else error
    
    
            put(Status.IN_OBJECT, Type.COMMA, { world: World, _: Token ->
                world
            })
            put(Status.IN_OBJECT, Type.VALUE, { world: World, token: Token ->
                world.pushAndSet(Status.PASSED_PAIR_KEY, token.value!!)
            })
            put(Status.IN_OBJECT, Type.RIGHT_BRACE, { world: World, _: Token ->
                if (world.hasValues()) {
                    world.popStatus()
                    world.popValue()
                    world.status = world.peekStatus()
                } else {
                    world.status = Status.IN_FINISHED_VALUE
                }
                world
            })
    
    
            put(Status.PASSED_PAIR_KEY, Type.COLON, { world: World, _: Token ->
                world
            })
            put(Status.PASSED_PAIR_KEY, Type.VALUE, { world: World, token: Token ->
                world.popStatus()
                val key = world.popValue() as String
                world.parent = world.getFirstObject()
                world.parent.put(key, token.value)
                world.status = world.peekStatus()
                world
            })
            put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { world: World, _: Token ->
                world.popStatus()
                val key = world.popValue() as String
                world.parent = world.getFirstObject()
                val newArray = JsonArray<Any>()
                world.parent.put(key, newArray)
                world.pushAndSet(Status.IN_ARRAY, newArray)
            })
            put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACE, { world: World, _: Token ->
                world.popStatus()
                val key = world.popValue() as String
                world.parent = world.getFirstObject()
                val newObject = JsonObject()
                world.parent.put(key, newObject)
                world.pushAndSet(Status.IN_OBJECT, newObject)
            })
            // else error
    
            put(Status.IN_ARRAY, Type.COMMA, { world: World, _: Token ->
                world
            })
            put(Status.IN_ARRAY, Type.VALUE, { world: World, token: Token ->
                val value = world.getFirstArray()
                value.add(token.value)
                world
            })
            put(Status.IN_ARRAY, Type.RIGHT_BRACKET, { world: World, _: Token ->
                if (world.hasValues()) {
                    world.popStatus()
                    world.popValue()
                    world.status = world.peekStatus()
                } else {
                    world.status = Status.IN_FINISHED_VALUE
                }
                world
            })
            put(Status.IN_ARRAY, Type.LEFT_BRACE, { world: World, _: Token ->
                val value = world.getFirstArray()
                val newObject = JsonObject()
                value.add(newObject)
                world.pushAndSet(Status.IN_OBJECT, newObject)
            })
            put(Status.IN_ARRAY, Type.LEFT_BRACKET, { world: World, _: Token ->
                val value = world.getFirstArray()
                val newArray = JsonArray<Any>()
                value.add(newArray)
                world.pushAndSet(Status.IN_ARRAY, newArray)
            })
        }
            // else error

        val lexer = Lexer(reader)

        var world = World(Status.INIT)
        do {
            val token = lexer.nextToken()
            log("Token: $token")
            world = sm.next(world, token)
        } while (token.tokenType != Type.EOF)

        return world.result
    }
}