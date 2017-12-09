package com.beust.klaxon;

import org.testng.annotations.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Test
class TestTypes {

    private fun getJsonObject(): JsonObject {
        val cls = TestTypes::class.java
        return Parser.parse(cls.getResourceAsStream("/types.json")!!) as JsonObject
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

    fun typeDouble(){
        val j = getJsonObject()
        assertEquals(12.34, j.double("double_value"))
    }

    fun typeDoubleExp(){
        val j = getJsonObject()
        assertEquals(3.141E-10, j.double("double_exp_value"))
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
        val cls = TestTypes::class.java
        val j = Parser.parse(cls.getResourceAsStream("/escaped.json")!!) as JsonObject
        assertEquals("""{"s":"text field \"s\"\nnext line\fform feed\ttab\\rev solidus/solidus\bbackspace"}""", j.toJsonString())
    }
}
