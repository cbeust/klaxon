package com.beust.klaxon

import java.io.File

fun main(args : Array<String>) {
    val name =
//            "/Users/cbeust/kotlin/klaxon/src/test/resources/c.json"
            "/d.json"
//            "/Users/cbeust/kotlin/klaxon/src/test/resources/b.json"
    val cls = javaClass<Parser2>()
    val inputStream = cls.getResourceAsStream(name)!!

    if (false) {
        val lexer = Lexer(inputStream)
        var token = lexer.nextToken()
        while (token.tokenType != Type.EOF) {
            println("Read : ${token}")
            token = lexer.nextToken()
        }
    } else {
        val jo = Parser2().parse(inputStream) as JsonArray
        println("JO: ${jo}")
        val john = jo?.find {
            (it as JsonObject).string("first") == "Simon"
        } as JsonObject
        println("Simon: ${john}")
        val scores = john?.obj("schoolResults")?.array("scores")?.filter {
//            val grade = it.get("grade")
            println("${it}")
            true
//            (it as JsonObject).long("grade")!! > 80
        }
//            ?.getArray()?.filter {
//            it.values().iterator().next().asLong() > 70
//        }
//                .forEach( {
//            it?.asLong()!! > 90
//        })
        println("Scores: ${scores}")
    }
}
