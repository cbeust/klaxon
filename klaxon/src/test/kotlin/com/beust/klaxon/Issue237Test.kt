package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertEquals

@Test
class Issue237Test {

    data class PlainObject(
        val a: Int,
        @Json(name = "filed_b")
        val b: String,
        val c: Float
    )

    fun issue237() {
        val aPlainObject = PlainObject(10, "test string", 3.141659f)

        val aJsonArray = JsonArray(listOf(
            JsonObject(mapOf("testing" to "Json")),
            JsonObject(mapOf("Array" to "Objects")),
            aPlainObject
        ))

        val aMix = json {
            obj (
                "theArray" to aJsonArray,
                "secondArray" to array(listOf("testing", "JsonArray", "Objects", "again"))
            )
        }

        assertEquals(
            "{\"theArray\": [{\"testing\": \"Json\"}, {\"Array\": \"Objects\"}, {\"a\" : 10, \"filed_b\" : \"test string\", \"c\" : 3.141659}], \"secondArray\": [\"testing\", \"JsonArray\", \"Objects\", \"again\"]}",
            Klaxon().toJsonString(aMix),
            "DefaultConverter failed to serialize a JsonArray within a Map."
        )
    }
}
