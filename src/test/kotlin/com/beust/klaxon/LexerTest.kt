package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.io.StringReader

@Test
class LexerTest {

    val expected = listOf(
            Token(Type.LEFT_BRACE),
            *value("a", 1),
            Token(Type.COMMA),
            *value("ab", 1),
            Token(Type.COMMA),
            *value("ab", 12),
            Token(Type.RIGHT_BRACE))

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
        = arrayOf(Token(Type.VALUE, name), Token(Type.COLON), Token(Type.VALUE, value))
}