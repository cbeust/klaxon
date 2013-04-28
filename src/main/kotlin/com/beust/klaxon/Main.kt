package com.beust.klaxon

import java.util.ArrayList

fun main(args : Array<String>) {
    val name =
//            "/Users/cbeust/kotlin/klaxon/src/test/resources/c.json"
            "/d.json"
//            "/Users/cbeust/kotlin/klaxon/src/test/resources/b.json"
    val cls = javaClass<Parser2>()
    val inputStream = cls.getResourceAsStream(name)!!

    val array = Parser2().parse(inputStream) as JsonArray<JsonObject>
    println("=== Finding Jack:")
    val jack = array?.find {
        it.string("first") == "Jack"
    }
    println("Simon: ${jack}")

    println("=== Everyone who studied in Berkeley:")
    val berkeley = array.filter {
        it.obj("schoolResults")?.string("location") == "Berkeley"
    }?.map {
        it.string("last")
    }
    println("${berkeley}")

    println("=== All last names:")
    val lastNames = array.string("last")
    println("${lastNames}")

    println("=== All grades bigger than 75")
    val result = array.flatMap {
        it.obj("schoolResults")
                ?.array(JsonObject(), "scores")?.filter {
                    it.long("grade")!! > 75
                }!!
    }
    println("Result: ${result}")
}
