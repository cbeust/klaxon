package com.beust.klaxon

import java.util.ArrayList

fun main(args : Array<String>) {
    example1()
//    example2()
//    example3()
}

fun parse(name: String) : Any {
    val cls = javaClass<Parser>()
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
    val array = parse("/e.json") as JsonArray<JsonObject>

    val ages = array.long("age")
    println("Ages: ${ages}")

    val oldPeople = array.filter {
        it.long("age")!! > 30
    }
    println("Old people: ${oldPeople}")
}

fun example1() {
    val array = parse("/d.json") as JsonArray<JsonObject>

    println("=== Finding Jack:")
    val jack = array.find {
        it.string("first") == "Jack"
    }
    println("Jack: ${jack}")

    println("=== Everyone who studied in Berkeley:")
    val berkeley = array.filter {
        it.obj("schoolResults")?.string("location") == "Berkeley"
    }.map {
        it.string("last")
    }
    println("${berkeley}")

    println("=== All last names:")
    val lastNames = array.string("last")
    println("${lastNames}")

    println("=== All grades bigger than 75")
    val result = array.map {
        it.obj("schoolResults")
                ?.array(JsonObject(), "scores")?.filter {
                    it.long("grade")!! > 75
                }!!
    }
    println("Result: ${result}")
}
