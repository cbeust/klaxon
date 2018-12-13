package com.beust.klaxon

import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.io.StringReader

data class Thinger(
        val width: Float?=null
)

class ThingerTest {
    @Test
    fun issue197() {
        val input = """{"width": 2}"""
        val thinger = Klaxon().parse<Thinger>(StringReader(input))!!

        assertEquals(thinger.width, 2f)
    }
}