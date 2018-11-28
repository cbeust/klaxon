package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class SpecialTypesTest {
    class MyEntity(
        @Json(name = "foo", ignored = true)
        var myFoo : String = "abc",

        @Json(name = "bar")
        var myBar : String
    )

    fun map() {
        val o = Klaxon().parse<Map<String, Any>>("""
            {
               "bar": "def"
               "entity": {
                   "bar": "isBar"
               }
            }
        """)
        assertThat(o!!.keys.size).isEqualTo(2)
        assertThat(o["bar"]).isEqualTo("def")
        assertThat((o["entity"] as JsonObject)["bar"]).isEqualTo("isBar")
    }


}