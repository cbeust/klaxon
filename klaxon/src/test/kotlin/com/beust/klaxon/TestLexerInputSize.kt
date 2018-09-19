package com.beust.klaxon

import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import kotlin.test.assertEquals

@Test
class TestLexerInputSize {
    @Test
    fun testInputNotReadFully() {
        var read = 0
        val PREFIX = "{\"a\":\""
        val first = ByteArrayInputStream(PREFIX.toByteArray(Charsets.UTF_8))
        val second: InputStream = object : InputStream() {
            var i = 40
            override fun read(): Int {
                if (i == -1) i++
                read++
                if (read > 2 * DEFAULT_BUFFER_SIZE) {
                    throw AssertionError("Was read ($read) more than expected" +
                            " (2 * DEFAULT_BUFFER_SIZE($DEFAULT_BUFFER_SIZE)) bytes")
                }
                return i++
            }
        }
        try {
            Parser.default().parse(SequenceInputStream(first, second)) as JsonObject
        } catch (e: RuntimeException) {
            assertEquals("Unexpected character at position 257: '#' (ASCII: 35)'", e.message)
        }
    }
}
