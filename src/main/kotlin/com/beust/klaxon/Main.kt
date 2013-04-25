package com.beust.klaxon

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
        val jo = Parser2().parse(inputStream)
        var results = jo.get("schoolResults")
        val scores = results?.get("scores")?.getArray()?.filter {
            it.values().iterator().next().asLong() > 70
        }
//                .forEach( {
//            it?.asLong()!! > 90
//        })
        println("Tests greater than 70: ${scores}")
    }
}
