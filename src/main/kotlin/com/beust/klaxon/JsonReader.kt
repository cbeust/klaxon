package com.beust.klaxon

import java.io.Reader

/**
 * Manages JSON streaming.
 */
class JsonReaderK(val reader: Reader) : Reader() {
    fun nextString() = consumeValue { value -> value.toString() }

    fun nextInt() = consumeValue { value -> value as Int }

    fun nextBoolean() = consumeValue { value -> value as Boolean }

    fun nextObject() : JsonObject {
        skip()
        return beginObject {
            val result = JsonObject()
            while (hasNext()) {
                val name = nextName()
                val value = consumeValue { value -> value }
                result[name] = value
            }
            result
        }
    }

    fun nextArray() : List<Any> {
        skip()
        val result = arrayListOf<Any>()
        return beginArray {
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


    /**
     * Makes sure that the next token is the beginning of an object (open brace),
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
     * @return true of this reader has more tokens to read before finishing the current object/array.
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

    fun nextName(): String {
        skip()
        val next = lexer.nextToken()
        if (next.tokenType == Type.VALUE) {
            return next.value.toString()
        } else {
            throw KlaxonException("Expected a name but got $next")
        }
    }

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
