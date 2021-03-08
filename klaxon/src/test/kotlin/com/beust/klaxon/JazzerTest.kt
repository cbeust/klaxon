package com.beust.klaxon

import org.testng.annotations.Test

class JazzerTest {
    @Test(expectedExceptions = [KlaxonException::class])
    fun characterInNumericLiteral() {
        val json = "0r"
        Parser.default().parse(StringBuilder(json))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun numericKeyAndObject() {
        val json = "{1{"
        Parser.default().parse(StringBuilder(json))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun numericKeyAndArray() {
        val json = "{3["
        Parser.default().parse(StringBuilder(json))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun numericKeyAndString() {
        val json = "{0\"\""
        Parser.default().parse(StringBuilder(json))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun incompleteUnicodeEscape() {
        val json = "\"\\u"
        Parser.default().parse(StringBuilder(json))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun nonNumericUnicodeEscape() {
        val json = "\"\\u\\\\{["
        Parser.default().parse(StringBuilder(json))
    }
}
