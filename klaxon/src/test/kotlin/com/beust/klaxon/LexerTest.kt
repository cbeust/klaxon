package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.io.StringReader

@Test
class LexerTest {

    val expected = listOf(
            Token(TokenType.LEFT_BRACE),
            *value("a", 1),
            Token(TokenType.COMMA),
            *value("ab", 1),
            Token(TokenType.COMMA),
            *value("ab", 12),
            Token(TokenType.RIGHT_BRACE))

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
        = arrayOf(Token(TokenType.VALUE, name), Token(TokenType.COLON), Token(TokenType.VALUE, value))
}