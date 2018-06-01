package com.beust.klaxon

import java.io.Reader
import java.math.BigInteger

/**
 * Manages JSON streaming.
 */
class JsonReader(val reader: Reader) : Reader() {
    /**
     * @return the next String.
     * @throws JsonParsingException the next value is not a String.
     */
    fun nextString() = consumeValue { value ->
        value as? String ?: throw JsonParsingException("Next token is not a string: $value")
    }

    /**
     * @return the next Int.
     * @throws JsonParsingException the next value is not an Int.
     */
    fun nextInt() = consumeValue { value ->
        value as? Int ?: throw JsonParsingException("Next token is not a int: $value")
    }

    /**
     * @return the next Long (upscaling as needed).
     * @throws JsonParsingException the next value is not an Long.
     */
    fun nextLong() = consumeValue { value ->
        when (value) {
            is Int -> value.toLong()
            is Long -> value
            else -> throw JsonParsingException("Next token is not a long: $value")
        }
    }

    /**
     * @return the next BigInteger (upscaling as needed).
     * @throws JsonParsingException the next value is not an BigInteger.
     */
    fun nextBigInteger() = consumeValue { value ->
        when (value) {
            is Int -> value.toBigInteger()
            is Long -> value.toBigInteger()
            is BigInteger -> value
            else -> throw JsonParsingException("Next token is not a big integer: $value")
        }
    }

    /**
     * @return the next Double (upscaling as needed).
     * @throws JsonParsingException the next value is not a Double.
     */
    fun nextDouble() = consumeValue { value ->
        when (value) {
            is Int -> value.toDouble()
            is Double -> value
            else -> throw JsonParsingException("Next token is not a double: $value")
        }
    }

    /**
     * @return the next boolean.
     * @throws JsonParsingException the next value is not a Boolean.
     */
    fun nextBoolean() = consumeValue { value ->
        value as? Boolean ?: throw JsonParsingException("Next token is not a boolean: $value")
    }

    /**
     * @return the next object, making sure the current token is an open brace and the last token is a closing brace.
     */
    fun nextObject() : JsonObject {
        return beginObject {
            JsonObject().let { result ->
                while (hasNext()) {
                    val name = nextName()
                    val value = consumeValue { value -> value }
                    result[name] = value
                }
                result
            }
        }
    }

    /**
     * @return the next array, making sure the current token is an open bracket and the last token is a closing bracket.
     */
    fun nextArray() : List<Any> {
        return beginArray {
            arrayListOf<Any>().let { result ->
                while (hasNext()) {
                    val v = consumeValue { value -> value }
                    if (v != null) {
                        result.add(v)
                    } else {
                        throw KlaxonException("Couldn't parse")
                    }
                }
                result
            }
        }
    }

    /**
     * @return the next name (the string left of a colon character).
     */
    fun nextName(): String {
        skip()
        val next = lexer.nextToken()
        if (next.tokenType == TokenType.VALUE) {
            return next.value.toString()
        } else {
            throw KlaxonException("Expected a name but got $next")
        }
    }

    /**
     * Make sure that the next token is the beginning of an object (open brace),
     * consume it, run the closure and then make sure the object is closed (closed brace).
     */
    fun <T> beginObject(closure: () -> T) : T {
        skip()
        privateBeginObject()
        val result = closure()
        privateEndObject()
        return result
    }

    /**
     * Makes sure that the next token is the beginning of an array (open bracket),
     * consume it, run the closure and then make sure the array is closed (closed bracket).
     */
    fun <T> beginArray(closure: () -> T) : T {
        skip()
        privateBeginArray()
        val result = closure()
        privateEndArray()
        return result
    }

    /**
     * @return true if this reader has more tokens to read before finishing the current object/array.
     */
    fun hasNext(): Boolean = lexer.peek().tokenType.let { it != TokenType.RIGHT_BRACKET && it != TokenType.RIGHT_BRACE }

    override fun close() {
        reader.close()
    }

    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        return reader.read(cbuf, off, len)
    }

    val lexer = Lexer(reader)

    private fun consumeToken(type: TokenType, expected: String) {
        val next = lexer.nextToken()
        if (next.tokenType != type) {
            throw KlaxonException("Expected a $expected but read $next")
        }
    }

    private fun privateBeginArray() = consumeToken(TokenType.LEFT_BRACKET, "[")
    private fun privateEndArray() = consumeToken(TokenType.RIGHT_BRACKET, "]")

    private fun privateBeginObject() = consumeToken(TokenType.LEFT_BRACE, "{")
    private fun privateEndObject() = consumeToken(TokenType.RIGHT_BRACE, "}")

    private val SKIPS = setOf(TokenType.COLON, TokenType.COMMA)
    private fun skip() {
        while (SKIPS.contains(lexer.peek().tokenType)) lexer.nextToken()
    }

    private fun <T> consumeValue(convert: (Any?) -> T): T {
        skip()

        val next = lexer.nextToken()
        if (next.tokenType == TokenType.VALUE) {
            return convert(next.value)
        } else {
            throw KlaxonException("Expected a name but got $next")
        }
    }

}
