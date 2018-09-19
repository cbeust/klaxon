package com.beust.klaxon

import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset

internal class KlaxonParser(
    private val pathMatchers: List<PathMatcher>,
    private val passedLexer: Lexer?,
    private val streaming: Boolean
) : Parser {
    override fun parse(rawValue: StringBuilder): Any? =
        StringReader(rawValue.toString()).use {
            parse(it)
        }

    override fun parse(inputStream: InputStream, charset: Charset): Any? {
        return parse(inputStream.reader(charset))
    }

    override fun parse(reader: Reader): Any? {
        return if (streaming) partialParseLoop(stateMachine, (reader as JsonReader).reader)
        else fullParseLoop(stateMachine, reader)
    }

    /**
     * A loop that ends either on an EOF or a closing brace or bracket (used in streaming mode).
     */
    private fun partialParseLoop(sm: StateMachine, reader: Reader): Any? {
        val lexer = passedLexer ?: Lexer(reader)

        var world = World(Status.INIT, pathMatchers)
        var wasNested: Boolean
        if (lexer.peek().tokenType == TokenType.COMMA) lexer.nextToken()
        do {
            val token = lexer.nextToken()
            log("Token: $token")
            wasNested = world.isNestedStatus()
            world = sm.next(world, token)
        } while (wasNested || (token.tokenType != TokenType.RIGHT_BRACE &&
                token.tokenType != TokenType.RIGHT_BRACKET &&
                token.tokenType != TokenType.EOF
                )
        )

        return world.popValue()
    }

    /**
     * A loop that only ends on EOF (used in non streaming mode).
     */
    private fun fullParseLoop(sm: StateMachine, reader: Reader): Any? {
        val lexer = passedLexer ?: Lexer(reader)

        var world = World(Status.INIT, pathMatchers)
        do {
            val token = lexer.nextToken()
            log("Token: $token")
            world.index = lexer.index
            world.line = lexer.line
            world = sm.next(world, token)
        } while (token.tokenType != TokenType.EOF)

        return world.result
    }

    //
    // Initialize the state machine
    //

    private val stateMachine = StateMachine(streaming)

    init {
        with(stateMachine) {
            put(
                Status.INIT,
                TokenType.VALUE, { world: World, token: Token ->
                world.pushAndSet(Status.IN_FINISHED_VALUE, token.value!!)
            })
            put(
                Status.INIT,
                TokenType.LEFT_BRACE, { world: World, _: Token ->
                world.pushAndSet(Status.IN_OBJECT, JsonObject())
            })
            put(
                Status.INIT,
                TokenType.LEFT_BRACKET, { world: World, _: Token ->
                world.pushAndSet(Status.IN_ARRAY, JsonArray<Any>())
            })
            // else error

            put(
                Status.IN_FINISHED_VALUE,
                TokenType.EOF, { world: World, _: Token ->
                world.result = world.popValue()
                world
            })
            // else error

            put(
                Status.IN_OBJECT,
                TokenType.COMMA, { world: World, _: Token ->
                world.foundValue()
                world
            })
            put(
                Status.IN_OBJECT,
                TokenType.VALUE, { world: World, token: Token ->
                world.pushAndSet(Status.PASSED_PAIR_KEY, token.value!!)
            })
            put(
                Status.IN_OBJECT,
                TokenType.RIGHT_BRACE, { world: World, _: Token ->
                world.foundValue()
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

            put(
                Status.PASSED_PAIR_KEY,
                TokenType.COLON, { world: World, _: Token ->
                world
            })
            put(
                Status.PASSED_PAIR_KEY,
                TokenType.VALUE, { world: World, token: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    parent[key] = token.value
                    status = peekStatus()
                    this
                }
            })
            put(
                Status.PASSED_PAIR_KEY,
                TokenType.LEFT_BRACKET, { world: World, _: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    val newArray = JsonArray<Any>()
                    parent[key] = newArray
                    pushAndSet(Status.IN_ARRAY, newArray)
                }
            })
            put(
                Status.PASSED_PAIR_KEY,
                TokenType.LEFT_BRACE, { world: World, _: Token ->
                with(world) {
                    popStatus()
                    val key = popValue() as String
                    parent = getFirstObject()
                    val newObject = JsonObject()
                    parent[key] = newObject
                    pushAndSet(Status.IN_OBJECT, newObject)
                }
            })
            // else error

            put(
                Status.IN_ARRAY,
                TokenType.COMMA, { world: World, _: Token ->
                world
            })
            put(
                Status.IN_ARRAY,
                TokenType.VALUE, { world: World, token: Token ->
                val value = world.getFirstArray()
                value.add(token.value)
                world
            })
            put(
                Status.IN_ARRAY,
                TokenType.RIGHT_BRACKET, { world: World, _: Token ->
                world.foundValue()
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
            put(
                Status.IN_ARRAY,
                TokenType.LEFT_BRACE, { world: World, _: Token ->
                val value = world.getFirstArray()
                val newObject = JsonObject()
                value.add(newObject)
                world.pushAndSet(Status.IN_OBJECT, newObject)
            })
            put(
                Status.IN_ARRAY,
                TokenType.LEFT_BRACKET, { world: World, _: Token ->
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
