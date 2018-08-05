package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

@Test
class Issue169 {

    data class Person(val id: Int,
                      val name: String,
                      val isEUResident: Boolean = false,
                      val city: String = "Paris"
                     )

    private val expected = Person(id = 2,
                                  name = "Arthur")

    fun test() {

        // language=JSON
        val jsonToTest = """
            {
              "id": 2,
              "name": "Arthur"
            }
        """.trimIndent()

        val toTest = Klaxon().parse<Person>(jsonToTest)!!

        assertThat(toTest.city)
                .isNotNull()
        assertThat(toTest)
                .isEqualTo(expected)
    }
}