package com.beust.klaxon

//import com.google.gson.stream.JsonReader
import org.testng.Assert
import org.testng.annotations.Test
import java.io.StringReader


@Test
class StreamingTest {
//    data class Person2(var name: String? = null, var age: Int? = null, var flag: Boolean? = false,
//            var array: List<Int> = emptyList())
//    fun streaming1() {
//        val jr = JsonReader(StringReader(array))
//    }

    fun streamingObject() {
        val objectString = """{
             "name": "Joe", "age": 23, "height": 1.85, "flag": true, "array": [1, 3],
             "obj1": { "a":1, "b":2 }
        }"""

        JsonReader(StringReader(objectString)).use { reader ->
            reader.beginObject() {
                var name: String? = null
                var age: Int? = null
                var height: Double? = null
                var flag: Boolean? = null
                var array: List<Any> = arrayListOf<Any>()
                var obj1: JsonObject? = null
                val expectedObj1 = JsonObject().apply {
                    this["a"] = 1
                    this["b"] = 2
                }
                while (reader.hasNext()) {
                    val readName = reader.nextName()
                    when (readName) {
                        "name" -> name = reader.nextString()
                        "age" -> age = reader.nextInt()
                        "height" -> height = reader.nextDouble()
                        "flag" -> flag = reader.nextBoolean()
                        "array" -> array = reader.nextArray()
                        "obj1" -> obj1 = reader.nextObject()
                        else -> Assert.fail("Expected either \"name\" or \"age\" but got $name")
                    }
                }
                Assert.assertEquals(name, "Joe")
                Assert.assertEquals(age, 23)
                Assert.assertEquals(height, 1.85)
                Assert.assertTrue(flag!!)
                Assert.assertEquals(array, listOf(1, 3))
                Assert.assertEquals(obj1, expectedObj1)
            }

        }
    }

    data class Person1(val name: String, val age: Int)
    val array = """[
            { "name": "Joe", "age": 23 },
            { "name": "Jill", "age": 35 }
        ]"""

    fun streamingArray() {
        val klaxon = Klaxon()
        JsonReader(StringReader(array)).use { reader ->
            val result = arrayListOf<Person1>()
            reader.beginArray {
                while (reader.hasNext()) {
                    val person = klaxon.parse<Person1>(reader)
                    result.add(person!!)
                }
                Assert.assertEquals(result, listOf(Person1("Joe", 23), Person1("Jill", 35)))
            }
        }
    }

    val arrayInObject = """{ "array": [
            { "name": "Joe", "age": 23 },
            { "name": "Jill", "age": 35 }
        ] }"""

    fun streamingArrayInObject() {
        val klaxon = Klaxon()
        JsonReader(StringReader(arrayInObject)).use { reader ->
            val result = arrayListOf<Person1>()
            reader.beginObject {
                val name = reader.nextName()
                Assert.assertEquals(name, "array")
                reader.beginArray {
                    while (reader.hasNext()) {
                        val person = klaxon.parse<Person1>(reader)
                        result.add(person!!)
                    }
                    Assert.assertEquals(result, listOf(Person1("Joe", 23), Person1("Jill", 35)))
                }
            }
        }
    }

    fun testNextString() {

        // String read normally
        JsonReader(StringReader("[\"text\"]")).use { reader ->
            val actual = reader.beginArray { reader.nextString() }
            Assert.assertEquals(actual, "text")
        }

        // null is not string
        assertParsingExceptionFromArray("[null]") {reader ->
            reader.nextString()
        }

        // Boolean is not string
        assertParsingExceptionFromArray("[true]") {reader ->
            reader.nextString()
        }

        // Int is not string
        assertParsingExceptionFromArray("[123]") {reader ->
            reader.nextString()
        }

        // Double is not string
        assertParsingExceptionFromArray("[0.123]") {reader ->
            reader.nextString()
        }

    }

    fun testNextInt() {

        // Int read normally
        JsonReader(StringReader("[0]")).use { reader ->
            val actual = reader.beginArray { reader.nextInt() }
            Assert.assertEquals(actual, 0)
        }

        // Double is not int
        assertParsingExceptionFromArray("[0.123]") {reader ->
            reader.nextInt()
        }

        // null is not int
        assertParsingExceptionFromArray("[null]") {reader ->
            reader.nextInt()
        }

        // String is not int
        assertParsingExceptionFromArray("[\"123\"]") {reader ->
            reader.nextInt()
        }

    }

    fun testNextDouble() {

        // Integer values should be autoconverted
        JsonReader(StringReader("[0]")).use { reader ->
            val actual = reader.beginArray { reader.nextDouble() }
            Assert.assertEquals(actual, 0.0)
        }

        // Native doubles
        JsonReader(StringReader("[0.123]")).use { reader ->
            val actual = reader.beginArray { reader.nextDouble() }
            Assert.assertEquals(actual, 0.123)
        }

        // NAN is not really specified
        assertParsingExceptionFromArray("[\"NAN\"]") {reader ->
            reader.nextDouble()
        }

        // Something entirely different from a double
        assertParsingExceptionFromArray("[null]") {reader ->
            reader.nextDouble()
        }

    }

    fun testNextBoolean() {

        // true read normally
        JsonReader(StringReader("[true]")).use { reader ->
            val actual = reader.beginArray { reader.nextBoolean() }
            Assert.assertEquals(actual, true)
        }

        // false read normally
        JsonReader(StringReader("[false]")).use { reader ->
            val actual = reader.beginArray { reader.nextBoolean() }
            Assert.assertEquals(actual, false)
        }

        // Int is not boolean
        assertParsingExceptionFromArray("[123]") {reader ->
            reader.nextBoolean()
        }

        // Double is not boolean
        assertParsingExceptionFromArray("[0.123]") {reader ->
            reader.nextBoolean()
        }

        // null is not boolean
        assertParsingExceptionFromArray("[null]") {reader ->
            reader.nextBoolean()
        }

        // String is not boolean
        assertParsingExceptionFromArray("[\"text\"]") {reader ->
            reader.nextBoolean()
        }

    }

    private fun assertParsingExceptionFromArray(json: String, nextValue: (JsonReader) -> Unit) {
        JsonReader(StringReader(json)).use { reader ->
            reader.beginArray {
                Assert.assertThrows(JsonParsingException::class.java) {
                    nextValue(reader)
                }
            }
        }

    }

//    fun streaming1() {
//        val reader = JsonReader(StringReader(array))//FileReader("src/test/resources/generated.json"))
//        reader.beginArray()
//        val gson = Gson()
////        gson.fromJson<>()
//        while (reader.hasNext()) {
//            val person = gson.fromJson<Person>(reader, Person::class.java)
//            println("Person:" + person)
//        }
//        reader.endArray()
//    }
}
