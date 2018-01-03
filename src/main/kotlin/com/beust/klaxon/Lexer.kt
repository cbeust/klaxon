package com.beust.klaxon

import java.io.Reader
import java.util.regex.Pattern

enum class Type {
    VALUE,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    COLON,
    EOF
}

class Token(val tokenType: Type, val value: Any?) {
    override fun toString() : String {
        val v =
            if (value != null) {
                " ($value)"
            } else {
                ""
            }
        return tokenType.toString() + v
    }
}

class Lexer(reader: Reader) {
    private val EOF = Token(Type.EOF, null)
    private var index = 1

    private val NUMERIC = Pattern.compile("[-]?[0-9]+")
    private val DOUBLE = Pattern.compile(NUMERIC.toString() + "((\\.[0-9]+)?([eE][-+]?[0-9]+)?)")!!

    private fun isSpace(c: Char): Boolean {
        return c == ' ' || c == '\r' || c == '\n' || c == '\t'
    }

    private val reader = reader.buffered()
    private var next: Char?

    init {
        val c = reader.read()
        next = if (c == -1) null else c.toChar()
    }

    private fun nextChar(): Char {
        if (isDone()) throw IllegalStateException("Cannot get next char: EOF reached")
        val c = next!!
        next = reader.read().let { if (it == -1) null else it.toChar() }
        index++
        return c
    }

    private fun peekChar() : Char {
        if (isDone()) throw IllegalStateException("Cannot peek next char: EOF reached")
        return next!!
    }

    private fun isDone() : Boolean = next == null

    val BOOLEAN_LETTERS = "falsetrue".toSet()
    private fun isBooleanLetter(c: Char) : Boolean {
        return BOOLEAN_LETTERS.contains(Character.toLowerCase(c))
    }

    val NULL_LETTERS = "null".toSet()

    fun isValueLetter(c: Char) : Boolean {
        return c == '-' || c == '+' || c == '.' || c.isDigit() || isBooleanLetter(c)
                || c in NULL_LETTERS
    }

    fun nextToken() : Token {

        if (isDone()) {
            return EOF
        }

        val tokenType: Type
        var c = nextChar()
        val currentValue = StringBuilder()
        var jsonValue: Any? = null

        while (! isDone() && isSpace(c)) {
            c = nextChar()
        }

        if ('"' == c) {
            tokenType = Type.VALUE
            loop@
            do {
                if (isDone()) {
                    throw RuntimeException("Unterminated string")
                }

                c = nextChar()
                when (c) {
                    '\\' -> {
                        if (isDone()) {
                            throw RuntimeException("Unterminated string")
                        }

                        c = nextChar()
                        when (c) {
                            '\\' -> currentValue.append("\\")
                            '/' -> currentValue.append("/")
                            'b' -> currentValue.append("\b")
                            'f' -> currentValue.append("\u000c")
                            'n' -> currentValue.append("\n")
                            'r' -> currentValue.append("\r")
                            't' -> currentValue.append("\t")
                            'u' -> {
                                val unicodeChar = StringBuilder(4)
                                    .append(nextChar())
                                    .append(nextChar())
                                    .append(nextChar())
                                    .append(nextChar())

                                val intValue = java.lang.Integer.parseInt(unicodeChar.toString(), 16)
                                currentValue.append(intValue.toChar())
                            }
                            else -> currentValue.append(c)
                        }
                    }
                    '"' -> break@loop
                    else -> currentValue.append(c)
                }
            } while (true)

            jsonValue = currentValue.toString()
        } else if ('{' == c) {
            tokenType = Type.LEFT_BRACE
        } else if ('}' == c) {
            tokenType = Type.RIGHT_BRACE
        } else if ('[' == c) {
            tokenType = Type.LEFT_BRACKET
        } else if (']' == c) {
            tokenType = Type.RIGHT_BRACKET
        } else if (':' == c) {
            tokenType = Type.COLON
        } else if (',' == c) {
            tokenType = Type.COMMA
        } else if (! isDone()) {
            while (isValueLetter(c)) {
                currentValue.append(c)
                if (! isValueLetter(peekChar())) {
                    break
                } else {
                    c = nextChar()
                }
            }
            val v = currentValue.toString()
            if (NUMERIC.matcher(v).matches()) {
                try {
                    jsonValue = java.lang.Integer.parseInt(v)
                } catch (e: NumberFormatException){
                    try {
                        jsonValue = java.lang.Long.parseLong(v)
                    } catch(e: NumberFormatException) {
                        jsonValue = java.math.BigInteger(v)
                    }
                }
            } else if (DOUBLE.matcher(v).matches()) {
                jsonValue = java.lang.Double.parseDouble(v)
            } else if ("true" == v.toLowerCase()) {
                jsonValue = true
            } else if ("false" == v.toLowerCase()) {
                jsonValue = false
            } else if (v == "null") {
                jsonValue = null
            } else {
                throw RuntimeException("Unexpected character at position ${index-1}"
                    + ": '$c' (ASCII: ${c.toInt()})'")
            }

            tokenType = Type.VALUE
        } else {
            tokenType = Type.EOF
        }

        return Token(tokenType, jsonValue)
    }
}
