package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertEquals

@Test
class Issue253Test {

    data class ObjWithNullAttr(
            val myAttr: Int?
    )

    fun issue253() {
        val obj = ObjWithNullAttr(null)
        val jsonStr = Klaxon().toJsonString(obj)
        assertEquals(
                """{"myAttr":null}""",
                jsonStr.replace(" ", "")
        )
        Klaxon().parse<ObjWithNullAttr>(jsonStr)
    }
}
