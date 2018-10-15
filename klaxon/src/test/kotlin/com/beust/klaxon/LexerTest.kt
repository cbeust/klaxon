package com.beust.klaxon

import com.beust.klaxon.token.*
import org.testng.Assert
import org.testng.annotations.Test
import java.io.StringReader

@Test
class LexerTest {

    val expected = listOf(
            LEFT_BRACE,
            *value("a", 1),
            COMMA,
            *value("ab", 1),
            COMMA,
            *value("ab", 12),
            RIGHT_BRACE
    )

    fun basic() {
        val s = """{
            "a": 1,
            "ab": 1,
            "ab": 12
        }"""
        testLexer(Lexer(StringReader(s)))
    }

    fun lenient() {
        val s = """{
            a : 1,
            ab: 1,
            ab: 12
            }
            """
        testLexer(Lexer(StringReader(s), lenient = true))
    }

    private fun testLexer(lexer: Lexer) {
        val result = Sequence{ -> lexer }.map { it }.toList()
        Assert.assertEquals(result, expected)
    }

    private fun value(name: String, value: Any): Array<Token>
        = arrayOf(Value(name), COLON, Value(value))
}