package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class Issue177 {

    data class UserData(val id: Int,
                      val name: String,
                      val role: String,
                      val additionalRole: String? = ""
                     )

    private val expected = UserData(1, "Jason", "SuperUser", null)

    fun test() {

        // language=JSON
        val jsonToTest = """
            {
              "id": 1,
              "name": "Jason",
              "role": "SuperUser",
              "additionalRole": null
            }
        """.trimIndent()

        val toTest = Klaxon().parse<UserData>(jsonToTest)

        toTest?.let{
            assertThat(toTest.additionalRole)
                    .isNull()
            assertThat(toTest)
                    .isEqualTo(expected)
        } ?: throw AssertionError("Expected object to be not null")

    }
}