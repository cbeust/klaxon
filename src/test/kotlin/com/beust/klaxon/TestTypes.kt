package com.beust.klaxon;

import com.beust.klaxon.jackson.jackson
import org.testng.annotations.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Test
class KlaxonTestTypes : BaseTestTypes() {
    override fun provideParser(): Parser =
        Parser.default()
}

@Test
class JacksonTestTypes : BaseTestTypes() {
    override fun provideParser(): Parser =
        Parser.jackson()
}

@Test
abstract class BaseTestTypes {
    protected abstract fun provideParser(): Parser

    private fun getJsonObject(): JsonObject {
        return read("/types.json") as JsonObject
    }

    private fun read(name: String): Any? {
        val cls = BaseTestTypes::class.java
        return provideParser().parse(cls.getResourceAsStream(name)!!)
    }

    fun typeInt() {
        val j = getJsonObject()
        assertEquals(123456789, j.int("int_value"))
    }

    fun typeLong() {
        val j = getJsonObject()
        assertEquals(2147483649, j.long("long_value"))
    }

    fun typeBigint() {
        val j = getJsonObject()
        assertEquals(BigInteger("123456789123456789123456789"), j.bigInt("bigint_value"))
    }

    fun typeBoolean() {
        val j = getJsonObject()
        assertEquals(false, j.boolean("boolean_value"))
    }

    fun typeFloat(){
        val j = getJsonObject()
        assertEquals(12.34f, j.float("float_value"))
    }

    fun typeFloatExp(){
        val j = getJsonObject()
        assertEquals(3.141E-10f, j.float("float_exp_value"))
    }

    fun typeString(){
        val j = getJsonObject()
        assertEquals("foo-bar", j.string("string_value"))
    }

    fun typeUnicode(){
        val j = getJsonObject()
        assertEquals("foo\u20ffbar", j.string("unicode_value"))
    }

    fun typeUnescapedUnicode(){
        val j = getJsonObject()
        val actual = j.string("unicode_unescaped_value")
        assertNotNull(actual)
        assertEquals(0x00FA, actual!![0].toInt())
        assertEquals(0x00FA, actual[1].toInt())
        assertEquals("úú", actual)
    }

    fun typeEscape(){
        val j = getJsonObject()
        assertEquals("[\"|\\|/|\b|\u000c|\n|\r|\t]", j.string("escape_value"))
    }

    fun typeObject(){
        val j = getJsonObject()
        assertEquals(JsonObject(), j.obj("object_value"))
    }

    fun typeArray(){
        val j = getJsonObject()
        assertEquals(JsonArray<Any>(), j.array<Any>("array_value"))
    }

    fun typeNull(){
        val j = getJsonObject()
        assertEquals(null, j.get("null_value"))
    }

    fun testEscapeRender(){
        val j = read("/escaped.json") as JsonObject
        assertEquals("""{"text field \"s\"\nnext line\fform feed\ttab\\rev solidus/solidus\bbackspace\u2018":"text field \"s\"\nnext line\fform feed\ttab\\rev solidus/solidus\bbackspace\u2018"}""", j.toJsonString())
    }
}
