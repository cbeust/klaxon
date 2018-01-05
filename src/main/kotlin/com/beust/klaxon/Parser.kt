package com.beust.klaxon

import java.io.*
import java.nio.charset.Charset

/**
 * Main entry for Klaxon's parser.
 *
 * If {streaming} is true, the parser doesn't expect to finish on an EOF token. This is used for streaming, when
 * the user requests to read a subset of the entire JSON document.
 */
class Parser(private val passedLexer: Lexer? = null, val streaming: Boolean = false) {
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
        return if (streaming) partialParseLoop(stateMachine, (reader as JsonReader).reader)
            else fullParseLoop(stateMachine, reader)
    }

    /**
     * A loop that ends either on an EOF or a closing brace or backet (used in streaming mode).
     */
    private fun partialParseLoop(sm: StateMachine, reader: Reader): Any? {
        val lexer = passedLexer ?: Lexer(reader)

        var world = World(Status.INIT)
        if (lexer.peek().tokenType == Type.COMMA) lexer.nextToken()
        do {
            val token = lexer.nextToken()
            log("Token: $token")
            world = sm.next(world, token)
        } while (token.tokenType != Type.RIGHT_BRACE && token.tokenType != Type.EOF)

        return world.popValue()
    }

    /**
     * A loop that only ends on EOF (used in non streaming mode).
     */
    private fun fullParseLoop(sm: StateMachine, reader: Reader): Any? {
        val lexer = passedLexer ?: Lexer(reader)

        var world = World(Status.INIT)
        do {
            val token = lexer.nextToken()
            log("Token: $token")
            world = sm.next(world, token)
        } while (token.tokenType != Type.EOF)

        return world.result
    }

    //
    // Initialize the state machine
    //

    private val stateMachine = StateMachine(streaming)
    init {
        with(stateMachine) {
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
                with(world) {
                    status = if (hasValues()) {
                        popStatus()
                        popValue()
                        peekStatus()
                    } else {
                        Status.IN_FINISHED_VALUE
                    }
                    this
                }
            })


            put(Status.PASSED_PAIR_KEY, Type.COLON, { world: World, _: Token ->
                world
            })
            put(Status.PASSED_PAIR_KEY, Type.VALUE, { world: World, token: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    parent.put(key, token.value)
                    status = peekStatus()
                    this
                }
            })
            put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACKET, { world: World, _: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    val newArray = JsonArray<Any>()
                    parent.put(key, newArray)
                    pushAndSet(Status.IN_ARRAY, newArray)
                }
            })
            put(Status.PASSED_PAIR_KEY, Type.LEFT_BRACE, { world: World, _: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    val newObject = JsonObject()
                    parent.put(key, newObject)
                    pushAndSet(Status.IN_OBJECT, newObject)
                }
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
                with(world) {
                    status = if (hasValues()) {
                        popStatus()
                        popValue()
                        peekStatus()
                    } else {
                        Status.IN_FINISHED_VALUE
                    }
                    this
                }
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
    }

    private fun log(s: String) {
        if (Debug.verbose) {
            println("[Parser] $s")
        }
    }
}