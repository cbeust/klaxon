package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertEquals

@Test
class Serialization {
    enum class Sause { ONION }
    val klaxon = Klaxon()

    private fun serializationTest(expected: String, actual: Any) {
        val actualSerialization = klaxon.toJsonString(actual)
        assertEquals(expected, actualSerialization)
    }

    @Test
    fun int() {
        serializationTest("1", 1)
    }

    @Test
    fun float() {
        serializationTest("0.55", 0.55f)
    }

    @Test
    fun double() {
        serializationTest("0.332", 0.332)
    }

    @Test
    fun boolean() {
        serializationTest("true", true)
    }

    @Test
    fun long() {
        serializationTest("200100", 200100L)
    }

    @Test
    fun string() {
        serializationTest("\"Onion Sauce !\"", "Onion Sauce !")
    }

    @Test
    fun enum() {
        serializationTest("\"ONION\"", Sause.ONION)
    }

    @Test
    fun collection() {
        val collection = listOf("mole", "ratty", "badger", "toad")
        serializationTest("[\"mole\", \"ratty\", \"badger\", \"toad\"]", collection)
    }

    @Test
    fun map() {
        val map = mapOf(1 to "uno", 2 to "dos", 3 to "tres")
        serializationTest("{\"1\": \"uno\", \"2\": \"dos\", \"3\": \"tres\"}", map)
    }
}
