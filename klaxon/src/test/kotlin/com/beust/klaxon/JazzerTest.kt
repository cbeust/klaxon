package com.beust.klaxon

import org.testng.annotations.Test

class JazzerTest {
    @Test(expectedExceptions = [KlaxonException::class])
    fun characterInNumericLiteral() {
        val json = "0r"
        Parser.default().parse(StringBuilder(json))
    }
}
