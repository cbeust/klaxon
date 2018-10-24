package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test(description = "Tests that null JSON values are correctly handled")
class Issue210 {
    data class DClass(val some: String, val none: String?)

    fun f() {
        val json = """
        {
        "some": "test",
        "none": null
        }
        """

        val p = Klaxon().parse<DClass>(json)
        assertThat(p!!.none).isNull()

    }
}
