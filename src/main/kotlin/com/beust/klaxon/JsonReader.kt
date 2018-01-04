package com.beust.klaxon

import java.io.Reader

/**
 * Manages JSON streaming.
 */
class JsonReaderK(val reader: Reader) : Reader() {
    /**
     * @return the next String.
     */
    fun nextString() = consumeValue { value -> value.toString() }

    /**
     * @return the next Int.
     */
    fun nextInt() = consumeValue { value -> value as Int }

    /**
     * @return the next boolean.
     */
    fun nextBoolean() = consumeValue { value -> value as Boolean }

    /**
     * @return the next object, making sure the current token is an open brace and the last token is a closing brace.
     */
    fun nextObject() : JsonObject {
        skip()
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
        skip()
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
        if (next.tokenType == Type.VALUE) {
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
        privateBeginArray()
        val result = closure()
        privateEndArray()
        return result
    }

    /**
     * @return true if this reader has more tokens to read before finishing the current object/array.
     */
    fun hasNext(): Boolean = lexer.peek().tokenType.let { it != Type.RIGHT_BRACKET && it != Type.RIGHT_BRACE }

    override fun close() {
        reader.close()
    }

    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        return reader.read(cbuf, off, len)
    }

    val lexer = Lexer(reader)

    private fun consumeToken(type: Type, expected: String) {
        val next = lexer.nextToken()
        if (next.tokenType != type) {
            throw KlaxonException("Expected a $expected but read $next")
        }
    }

    private fun privateBeginArray() = consumeToken(Type.LEFT_BRACKET, "[")
    private fun privateEndArray() = consumeToken(Type.RIGHT_BRACKET, "]")

    private fun privateBeginObject() = consumeToken(Type.LEFT_BRACE, "{")
    private fun privateEndObject() = consumeToken(Type.RIGHT_BRACE, "}")

    private val SKIPS = setOf(Type.COLON, Type.COMMA)
    private fun skip() {
        while (SKIPS.contains(lexer.peek().tokenType)) lexer.nextToken()
    }

    private fun <T> consumeValue(convert: (Any?) -> T): T {
        skip()

        val next = lexer.nextToken()
        if (next.tokenType == Type.VALUE) {
            return convert(next.value)
        } else {
            throw KlaxonException("Expected a name but got $next")
        }
    }

}
