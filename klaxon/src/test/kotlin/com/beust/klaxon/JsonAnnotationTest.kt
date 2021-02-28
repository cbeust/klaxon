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
    fun serializeNullTest() {
        data class ObjWithSerializeNullFalse(
            @Json(serializeNull = false)
            val value: Int?
        )

        Assertions
            .assertThat(
                Klaxon().toJsonString(
                    ObjWithSerializeNullFalse(null)
                )
            )
            .isEqualTo("{}") // with serializeNull = false, the field is not serialized

        Assertions
            .assertThat(
                Klaxon().toJsonString(
                    ObjWithSerializeNullFalse(1)
                )
            )
            .isEqualTo("""{"value" : 1}""")

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

    // TODO The following tests show some tricky examples with parsing and defaults

    @Test
    fun serializeNullFalseParse() {
        // when serializeNull = false, empty field in JSON should default to null value
        data class ObjWithSerializeNullFalse(
            @Json(serializeNull = false)
            val value: Int?
        )

        val originalObj = ObjWithSerializeNullFalse(null)
        val serialized = Klaxon().toJsonString(originalObj)
        Assert.assertEquals("{}", serialized)
        val parsed = Klaxon().parse<ObjWithSerializeNullFalse>(serialized)
        val expected = ObjWithSerializeNullFalse(null)

        Assert.assertEquals(expected, parsed)
    }

    @Test
    fun serializeNullFalseParseWithDefault() {
        // when a default is set, it should override the null default from serializeNull
        data class ObjWithSerializeNullFalseAndDefault(
            @Json(serializeNull = false)
            val value: Int? = 1
        )

        val originalObj = ObjWithSerializeNullFalseAndDefault(null)
        val serialized = Klaxon().toJsonString(originalObj)
        Assert.assertEquals("{}", serialized)

        val parsed = Klaxon().parse<ObjWithSerializeNullFalseAndDefault>(serialized)
        val expected = ObjWithSerializeNullFalseAndDefault(1)

        Assert.assertEquals(expected, parsed)
        // The roundtrip object-json-object does not lead to the same object!
    }

    // Another variant that the previous test could go
    @Test
    fun serializeNullFalseParseWithDefaultAlternative() {
        // Kotlin defaults are ignored when serializeNull == false
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
        // The roundtrip object-json-object leads to the same object
    }
}