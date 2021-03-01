package com.beust.klaxon

import org.assertj.core.api.Assertions
import org.testng.Assert
import org.testng.annotations.Test

@Test
class JsonAnnotationTest {
    private val jsonString: String = json { obj(
            "name" to "John",
            "change" to 1
    ) }.toJsonString()

    fun ignoredWithAnnotation() {
        class IgnoredWithAnnotation(
                val name: String,
                @Json(ignored = true)
                val change: Int = 0)

        val result = Klaxon().parse<IgnoredWithAnnotation>(jsonString)
        Assert.assertEquals(result?.name, "John")
        Assert.assertEquals(result?.change, 0)
    }

    fun ignoredWithPrivate() {
        class IgnoredWithPrivate(
                val name: String,
                private val change: Int = 0){
            fun changed(): Boolean = change != 0
        }

        val result = Klaxon().parse<IgnoredWithPrivate>(jsonString)
        Assert.assertEquals(result?.name, "John")
        Assert.assertEquals(result?.changed(), false)
    }

    @Test
    fun privateNotIgnored() {
        data class Config(
                val version: String,
                @Json(ignored = false)
                private val projects: Set<String>) {
            fun contains(name: String) = projects.contains(name)
        }

        val jsonString = """{"version": "v1", "projects": ["abc"]}"""
        val r = Klaxon().parse<Config>(jsonString)
        Assertions.assertThat(r).isEqualTo(Config("v1", setOf("abc")))

    }

    @Test
    fun serializeNullFalseRoundtripWithoutDefault() {
        // when serializeNull == false, null is the default value during parsing
        data class ObjWithSerializeNullFalse(
            @Json(serializeNull = false)
            val value: Int?
        )

        val originalObj = ObjWithSerializeNullFalse(null)
        val serialized = Klaxon().toJsonString(originalObj)
        Assert.assertEquals("{}", serialized) // with serializeNull = false, the null property is not serialized
        val parsed = Klaxon().parse<ObjWithSerializeNullFalse>(serialized)
        val expected = ObjWithSerializeNullFalse(null)

        Assert.assertEquals(expected, parsed)
    }

    @Test
    fun serializeNullFalseRoundtripWithDefault() {
        // Kotlin defaults are ignored when serializeNull == false and replaced with null during parsing
        data class ObjWithSerializeNullFalseAndDefault(
            @Json(serializeNull = false)
            val value: Int? = 1
        )

        val originalObj = ObjWithSerializeNullFalseAndDefault(null)
        val serialized = Klaxon().toJsonString(originalObj)
        Assert.assertEquals("{}", serialized)
        val parsed = Klaxon().parse<ObjWithSerializeNullFalseAndDefault>(serialized)
        val expected = ObjWithSerializeNullFalseAndDefault(null)

        Assert.assertEquals(expected, parsed)
    }

    @Test
    fun serializeNullFalseValueSet() {
        data class ObjWithSerializeNullFalse(
            @Json(serializeNull = false)
            val value: Int?
        )

        Assertions
            .assertThat(
                Klaxon().toJsonString(
                    ObjWithSerializeNullFalse(1)
                )
            )
            .isEqualTo("""{"value" : 1}""")
    }

    @Test
    fun serializeNullTrue() {
        data class ObjWithSerializeNullTrue(
            @Json(serializeNull = true)
            val value: Int?
        )

        Assertions
            .assertThat(
                Klaxon().toJsonString(
                    ObjWithSerializeNullTrue(null)
                )
            )
            .isEqualTo("""{"value" : null}""")

        Assertions
            .assertThat(
                Klaxon().toJsonString(
                    ObjWithSerializeNullTrue(1)))
            .isEqualTo("""{"value" : 1}""")
    }

    @Test
    fun serializeNullWithoutNullableProperty() {
        data class ObjWithSerializeNullFalse(
            @Json(serializeNull = false)
            val value: Int = 1
        )

        val parsed = Klaxon().parse<ObjWithSerializeNullFalse>("{}")
        val expected = ObjWithSerializeNullFalse(1)

        Assert.assertEquals(expected, parsed)
    }
}