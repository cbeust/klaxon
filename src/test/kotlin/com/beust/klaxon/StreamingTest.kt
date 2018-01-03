package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test
import java.io.StringReader


@Test
class StreamingTest {
    data class Person(var name: String? = null, var age: Int? = null)

    fun streaming2() {
        val array = """[
            { "name": "Joe", "age": 23 },
            { "name": "Jill", "age": 35 }
        ]"""

        val klaxon = Klaxon()

        JsonReaderK(StringReader(array)).use { reader ->
            val result = arrayListOf<Person>()
            reader.beginArray()
            while (reader.hasNext()) {
                val person = klaxon.parse<Person>(reader)
                result.add(person!!)
            }
            reader.endArray()
            Assert.assertEquals(result, listOf(Person("Joe", 23), Person("Jill", 35)))
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