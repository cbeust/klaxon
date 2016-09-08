package com.beust.klaxon.test

import java.util.ArrayList
import com.beust.klaxon.*

fun main(args: Array<String>) {
//    val a1 = json {
//        obj("a", 1.1, "b", "value", "c", array(1))
//    }
//    val a2 = json {
//        obj("a", 1.1, "b", "value", "c", array(1))
//    }
//    val result = a1.equals(a2)
//    println("Equals: ${result}")

//    foo("a")
//    example1()
//    example2()
//    example3()

//    val parser: Parser = Parser()
//    val stringBuilder: StringBuilder = StringBuilder("{\"name\":\"Sakib Sami\", \"age\":23}")
//    val json: JsonObject = parser.parse(stringBuilder) as JsonObject
//    println("Name : ${json.string("name")}, Age : ${json.int("age")}")

    val anObject = json {
        obj("a" to 1, "b" to "value")
    }
    println("Json object: ${anObject.toJsonString()}")

    val anArray = json {
        array("a", 1, false)
    }
    println("Json array: ${anArray.toJsonString()}")

    val aMix = json {
        obj(
                "theArray" to anArray,
                "theObject" to anObject,
                "anInt" to 4
        )
    }
    println("Mix: ${aMix.toJsonString()}")

    println("=== Logic into the DSL")
    val logic = json {
        array(arrayListOf(1, 2, 3).map {
            obj(it.toString() to it)
        })
    }
    println("Result: ${logic.toJsonString()}")

}

fun parse(name: String): Any {
    val cls = Parser::class.java
    val inputStream = cls.getResourceAsStream(name)!!
    return Parser().parse(inputStream)!!
}

fun example3() {
    val obj = parse("/object.json") as JsonObject

    val firstName = obj.string("firstName")
    val lastName = obj.string("lastName")
    println("Name: ${firstName} ${lastName}")
}

fun example2() {
    val array = parse("/e.json") as JsonArray<*>

    val ages = array.long("age")
    println("Ages: ${ages}")

    val oldPeople = array.filter {
        it is JsonObject && it.long("age")?.let { it > 30 } ?: false
    }
    println("Old people: ${oldPeople}")
}

fun example1() {
    val array = parse("/d.json") as JsonArray<*>

    println("=== Finding Jack:")
    val jack = array.first {
        it is JsonObject && it.string("first") == "Jack"
    }
    println("Jack: ${jack}")

    println("=== Everyone who studied in Berkeley:")
    val berkeley = array.filterIsInstance<JsonObject>().filter {
        it.obj("schoolResults")?.string("location") == "Berkeley"
    }.map {
        it.string("last")
    }
    println("${berkeley}")

    println("=== All last names:")
    val lastNames = array.string("last")
    println("${lastNames}")

    println("=== All grades bigger than 75")
    val result = array.filterIsInstance<JsonObject>().map {
        it.obj("schoolResults")
                ?.array<JsonObject>("scores")?.filter {
            it.long("grade")!! > 75
        }!!
    }
    println("Result: ${result}")
}
