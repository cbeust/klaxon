package com.beust.klaxon

import org.testng.annotations.Test
import java.time.Instant

class Issue84 {
    /**
     * This test passes.
     */
    @Test
    fun serializeNestedInstant() {
        data class Person(val firstName: String, val dob: Instant)

        class EpochMilliInstantConverter: Converter<Instant> {
            override fun toJson(value: Instant) = value.toEpochMilli().toString()
            override fun fromJson(jv: JsonValue) = throw NotImplementedError()
        }

        // When no custom converted is provided, empty value for dob.  This used to throw an exception.  Not sure what intended operation is
        val obj = Person("John", Instant.ofEpochMilli(9001))
        BindingTest.assertContains(Klaxon().toJsonString(obj), "9001")
        // Actual: {"dob" : {}, "firstName" : "John"}

        // When custom converted is provided, dob is serialized as expected
        val mapper = Klaxon().converter(EpochMilliInstantConverter())
        BindingTest.assertContains(mapper.toJsonString(obj), "9001")
        // Actual: {"dob" : 9001, "firstName" : "John"}
    }

    /**
     * This test does not pass.
     */
    @Test
    fun serializeListOfInstants() {
        val dates = listOf(Instant.ofEpochMilli(9001))

        class EpochMilliInstantConverter: Converter<Instant> {
            override fun toJson(value: Instant) = value.toEpochMilli().toString()
            override fun fromJson(jv: JsonValue) = throw NotImplementedError()
        }

        // despite custom converter being provided, instant is not serialized.  Empty value in list
        val mapper = Klaxon().converter(EpochMilliInstantConverter())
        val result = mapper.toJsonString(dates)
        BindingTest.assertContains(result, "9001")
        // Actual: [{}]
    }
}