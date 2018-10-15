package com.beust.klaxon

import com.beust.klaxon.token.*
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
    fun nextString() = consumeValue<String>()

    /**
     * @return the next Int.
     * @throws JsonParsingException the next value is not an Int.
     */
    fun nextInt() = consumeValue<Int>()

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
            is Int -> BigInteger.valueOf(value.toLong())
            is Long -> BigInteger.valueOf(value)
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
    fun nextBoolean() = consumeValue<Boolean>()

    /**
     * @return the next object, making sure the current token is an open brace and the last token is a closing brace.
     */
    fun nextObject() : JsonObject {
        return beginObject {
            JsonObject().let { result ->
                while (hasNext()) {
                    val name = nextName()
                    val value = consumeValue<Any?>()
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
                    val v = consumeValue<Any?>()
                    v?.let { result.add(it) } ?: throw KlaxonException("Couldn't parse")
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
        if (next !is Value<*> || next.value !is String) {
            throw KlaxonException("Expected a name but got $next")
        }
        return next.value
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
    fun hasNext(): Boolean = lexer.peek().let { it !is RIGHT_BRACKET && it !is RIGHT_BRACE }

    override fun close() {
        reader.close()
    }

    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        return reader.read(cbuf, off, len)
    }

    val lexer = Lexer(reader)

    private inline fun <reified T : Token> consumeToken() {
        val next = lexer.nextToken()
        if (next !is T) {
            throw KlaxonException("Expected a ${T::class.objectInstance.toString()} but read $next")
        }
    }

    private fun privateBeginArray() = consumeToken<LEFT_BRACKET>()
    private fun privateEndArray() = consumeToken<RIGHT_BRACKET>()

    private fun privateBeginObject() = consumeToken<LEFT_BRACE>()
    private fun privateEndObject() = consumeToken<RIGHT_BRACE>()

    private val SKIPS = setOf(COLON, COMMA)
    private fun skip() {
        while (SKIPS.contains(lexer.peek())) lexer.nextToken()
    }

    private fun nextValue(): Any? {
        skip()

        val next = lexer.nextToken()
        if (next !is Value<*>) {
            throw KlaxonException("Expected a value but got $next")
        }
        return next.value
    }

    private fun <T> consumeValue(convert: (Any?) -> T): T {
        return convert(nextValue())
    }

    /**
     * Convenience method for consuming a primitive value as is (without any conversion needed).
     */
    private inline fun <reified T> consumeValue(): T {
        val value = nextValue()
        return value as? T
            ?: throw JsonParsingException("Next token is not a ${T::class.java.simpleName}: $value")
    }
}
