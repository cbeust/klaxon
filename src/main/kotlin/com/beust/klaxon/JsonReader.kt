package com.beust.klaxon

import java.io.Reader

class JsonReaderK(val reader: Reader) : Reader() {
    override fun close() {
        reader.close()
    }

    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        return reader.read(cbuf, off, len)
    }

    val lexer = Lexer(reader)

    fun beginArray() {
        val next = lexer.nextToken()
        if (next.tokenType != Type.LEFT_BRACKET) {
            throw KlaxonException("Expected a '[' but read $next")
        }
    }

    fun endArray() {
        val next = lexer.nextToken()
        if (next.tokenType != Type.RIGHT_BRACKET) {
            throw KlaxonException("Expected a ']' but read $next")
        }
    }

    fun hasNext(): Boolean = lexer.peek().tokenType.let { it != Type.RIGHT_BRACKET && it != Type.RIGHT_BRACE }
}
