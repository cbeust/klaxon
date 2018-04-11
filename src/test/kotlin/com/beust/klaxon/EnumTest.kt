package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test

@Test
class EnumTest {

    val convertColor = object: Converter {
        override fun canConvert(cls: Class<*>) = cls == Color::class.java

        override fun toJson(value: Any): String = when(value as Color) {
            Color.R -> "red"
            Color.G -> "green"
            Color.B -> "blue"
            else -> throw IllegalArgumentException("Unknown color")
        }

        override fun fromJson(jv: JsonValue): Color = when(jv.inside) {
            "red" -> Color.R
            "green" -> Color.G
            "blue" -> Color.B
            else -> throw IllegalArgumentException("Invalid Color")
        }
    }

    enum class Color { R, G, B }
    data class Root (val colors: List<Color>)

    fun listOfEnums() {
        val klaxon = Klaxon().converter(convertColor)
        val result = klaxon.parse<Root>("""
        {
            "colors": ["red", "green", "blue"]
        }
        """)
    }

    enum class Cardinal { NORTH, SOUTH }
    class Direction(var cardinal: Cardinal? = null)
    fun enum() {
        val result = Klaxon().parse<Direction>("""
            { "cardinal": "NORTH" }
        """
        )
        Assert.assertEquals(result?.cardinal, Cardinal.NORTH)
    }


}