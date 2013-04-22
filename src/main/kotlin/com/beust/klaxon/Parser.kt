package com.beust.klaxon

import java.util.regex.Pattern
import java.util.LinkedList

public enum class Status {
    INIT;
    IN_FINISHED_VALUE;
    IN_OBJECT;
    IN_ARRAY;
    PASSED_PAIR_KEY;
    PAIR_VALUE;
    END;
    IN_ERROR;
}

/**
 * @author Cedric Beust <cedric@beust.com>
 * @since 04 20, 2013
 */
public class Parser {
    var status = Status.INIT

    public fun run(fileName: String) : JsonObject {
        var result = JsonObject()
        val lexer = Lexer(fileName)

        val statusStack = LinkedList<Status>()
        val valueStack = LinkedList<JsonObject>()
        var done = false

        do {
            val token = lexer.nextToken()
            val tokenType = token.tokenType

            println("Current token: ${token}")
            if (status == Status.INIT) {
                if (tokenType == Type.VALUE) {
                    status = Status.IN_FINISHED_VALUE
                    statusStack.addFirst(status)
                    valueStack.addFirst(token.value!!)
                } else if (tokenType == Type.LEFT_BRACE) {
                    status = Status.IN_OBJECT
                    statusStack.addFirst(status);
                    valueStack.addFirst(JsonObject())
                } else if (tokenType == Type.LEFT_BRACKET) {
                    status = Status.IN_ARRAY;
                    statusStack.addFirst(status);
                    valueStack.addFirst(JsonArray())
                } else {
                    status = Status.IN_ERROR
                }

            } else if (status == Status.IN_FINISHED_VALUE) {
                if (token.tokenType == Type.END) {
                    result = valueStack.removeFirst()
                    done = true
                } else {
                    throw RuntimeException("Unexpected token")
                }

            } else if (status == Status.IN_OBJECT) {
                if (tokenType == Type.COMMA) {
                } else if (tokenType == Type.VALUE) {
                    val key = token.value;
                    valueStack.addFirst(key!!);
                    status = Status.PASSED_PAIR_KEY;
                    statusStack.addFirst(status);
                } else if (tokenType == Type.RIGHT_BRACE) {
                    if(valueStack.size() > 1){
                        statusStack.removeFirst()
                        valueStack.removeFirst()
                        status = statusStack.get(0)
                    }
                    else{
                        status = Status.IN_FINISHED_VALUE;
                    }
                }

            } else if (status == Status.PASSED_PAIR_KEY) {
                val key: String?
                val parent: JsonObject
                if (tokenType == Type.COLON) {
                } else if (tokenType == Type.VALUE) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst().asString()
                    parent = valueStack.getFirst()
                    parent.put(key!!, token.value!!)
                    status = statusStack.get(0)
                } else if (tokenType == Type.LEFT_BRACKET) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst().asString()
                    parent = valueStack.getFirst()
                    val newArray = JsonArray()
                    parent.put(key!!, newArray)
                    status = Status.IN_ARRAY
                    statusStack.addFirst(status)
                    valueStack.addFirst(newArray);
                } else if (tokenType == Type.LEFT_BRACE) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst().asString()
                    parent = valueStack.getFirst()
                    val newObject = JsonObject()
                    parent.put(key!!, newObject)
                    status = Status.IN_OBJECT
                    statusStack.addFirst(status)
                    valueStack.addFirst(newObject)
                } else {
                    status = Status.IN_ERROR
                }
            } else if (status == Status.IN_ARRAY) {
                if (tokenType == Type.COMMA) {
                } else if (tokenType == Type.VALUE) {
                    val value = valueStack.getFirst() as JsonArray
                    value.add(token.value!!)
                } else if (tokenType == Type.RIGHT_BRACKET) {
                    if (valueStack.size() > 1){
                        statusStack.removeFirst()
                        valueStack.removeFirst()
                        status = statusStack.get(0) // peek
                    }
                    else{
                        status = Status.IN_FINISHED_VALUE;
                    }

                } else if (tokenType == Type.LEFT_BRACE) {
                    val value = valueStack.getFirst() as JsonArray
                    val newObject = JsonObject()
                    value.add(newObject)
                    status = Status.IN_OBJECT
                    statusStack.addFirst(status)
                    valueStack.addFirst(newObject)
                } else if (tokenType == Type.LEFT_BRACKET) {
                    val value = valueStack.getFirst() as JsonArray
                    val newArray = JsonArray()
                    value.add(newArray)
                    status =Status.IN_ARRAY
                    statusStack.addFirst(status)
                    valueStack.addFirst(newArray)
                } else {
                    status = Status.IN_ERROR
                }
            } else if (status == Status.IN_ERROR) {
                throw RuntimeException("Unexpected token: " + token)
            }

        } while (token.tokenType != Type.END)


        println("Returning: ${result}")
        return result
    }
}

fun main(args : Array<String>) {
    val fileName = "/tmp/e.json"
    if (false) {
        val lexer = Lexer(fileName)
        var token = lexer.nextToken()
        while (token.tokenType != Type.END) {
            println("Read : ${token}")
            token = lexer.nextToken()
        }
    } else {
        Parser().run(fileName)
    }
}