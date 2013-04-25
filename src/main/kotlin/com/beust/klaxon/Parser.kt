package com.beust.klaxon

import java.util.LinkedList
import java.io.InputStream

public enum class Status {
    INIT;
    IN_FINISHED_VALUE;
    IN_OBJECT;
    IN_ARRAY;
    PASSED_PAIR_KEY;
    PAIR_VALUE;
    IN_ERROR;
    EOF;
}

/**
 * @author Cedric Beust <cedric@beust.com>
 * @since 04 20, 2013
 */
public class Parser {
    var status = Status.INIT
    var verbose = false

    fun log(s : String) {
        if (verbose) {
            println(s)
        }
    }

    public fun parse(inputStream : InputStream) : JsonObject {
        var result = JsonObject()
        val lexer = Lexer(inputStream)

        val statusStack = LinkedList<Status>()
        val valueStack = LinkedList<JsonObject>()

        do {
            val token = lexer.nextToken()
            val tokenType = token.tokenType

            log("Current token: ${token}")
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
                if (token.tokenType == Type.EOF) {
                    result = valueStack.removeFirst()
                } else {
                    throw RuntimeException("Unexpected token: ${token}")
                }

            } else if (status == Status.IN_OBJECT) {
                if (tokenType == Type.COMMA) {
                } else if (tokenType == Type.VALUE) {
                    val key = token.value
                    valueStack.addFirst(key!!)
                    status = Status.PASSED_PAIR_KEY
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
                val key: JsonString?
                val parent: JsonObject
                if (tokenType == Type.COLON) {
                } else if (tokenType == Type.VALUE) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst() as JsonString
                    parent = valueStack.getFirst()
                    parent.put(key!!.asString(), token.value!!)
                    status = statusStack.get(0)
                } else if (tokenType == Type.LEFT_BRACKET) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst() as JsonString
                    parent = valueStack.getFirst()
                    val newArray = JsonArray()
                    parent.put(key!!.asString(), newArray)
                    status = Status.IN_ARRAY
                    statusStack.addFirst(status)
                    valueStack.addFirst(newArray);
                } else if (tokenType == Type.LEFT_BRACE) {
                    statusStack.removeFirst()
                    key = valueStack.removeFirst() as JsonString
                    parent = valueStack.getFirst()
                    val newObject = JsonObject()
                    parent.put(key!!.asString(), newObject)
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
                    if (valueStack.size() > 1) {
                        statusStack.removeFirst()
                        valueStack.removeFirst()
                        status = statusStack.get(0) // peek
                    }
                    else{
                        status = Status.IN_FINISHED_VALUE
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
                    status = Status.IN_ARRAY
                    statusStack.addFirst(status)
                    valueStack.addFirst(newArray)
                } else {
                    status = Status.IN_ERROR
                }
            } else if (status == Status.IN_ERROR) {
                throw RuntimeException("Unexpected token: " + token)
            }

        } while (token.tokenType != Type.EOF)

        return result
    }
}

