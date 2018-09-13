package com.beust.klaxon

import java.io.Reader
import java.util.regex.Pattern

enum class TokenType(val value: String) {
    VALUE("a value"),
    LEFT_BRACE("\"{\""),
    RIGHT_BRACE("\"}\""),
    LEFT_BRACKET("\"[\""),
    RIGHT_BRACKET("\"]\""),
    COMMA("\",\""),
    COLON("\":\""),
    EOF("EOF")
}

data class Token(val tokenType: TokenType, val value: Any? = null) {
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

/**
 * if `lenient` is true, names (the identifiers left of the colon) are allowed to not be surrounded by double quotes.
 */
class Lexer(val passedReader: Reader, val lenient: Boolean = false): Iterator<Token> {
    private val EOF = Token(TokenType.EOF, null)
    var index = 0
    var line = 1

    private val NUMERIC = Pattern.compile("[-]?[0-9]+")
    private val DOUBLE = Pattern.compile(NUMERIC.toString() + "((\\.[0-9]+)?([eE][-+]?[0-9]+)?)")!!

    private fun isSpace(c: Char): Boolean {
        if (c == '\n') line++
        return c == ' ' || c == '\r' || c == '\n' || c == '\t'
    }

    private val reader = passedReader.buffered()
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
//        log("Next char: '$c' index:$index")
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

    private var peeked: Token? = null

    fun peek() : Token {
        if (peeked == null) {
            peeked = actualNextToken()
        }
        return peeked!!
    }

    override fun next() = nextToken()
    override fun hasNext() = peek() != EOF

    fun nextToken(): Token {
        val result =
            if (peeked != null) {
                val r = peeked!!
                peeked = null
                r
            } else {
                actualNextToken()
            }
        return result
    }

    private var expectName = false

    private fun actualNextToken() : Token {

        if (isDone()) {
            return EOF
        }

        val tokenType: TokenType
        var c = nextChar()
        val currentValue = StringBuilder()
        var jsonValue: Any? = null

        while (! isDone() && isSpace(c)) {
            c = nextChar()
        }

        if ('"' == c || (lenient && expectName)) {
            if (lenient) {
                currentValue.append(c)
            }
            tokenType = TokenType.VALUE
            loop@
            do {
                if (isDone()) {
                    throw KlaxonException("Unterminated string")
                }

                c = if (lenient) peekChar() else nextChar()
                when (c) {
                    '\\' -> {
                        if (isDone()) {
                            throw KlaxonException("Unterminated string")
                        }

                        c = nextChar()
                        when (c) {
                            '\\' -> currentValue.append("\\")
                            '/' -> currentValue.append("/")
                            'b' -> currentValue.append("\b")
                            'f' -> currentValue.append("\u000c")
                            'n' -> {
                                currentValue.append("\n")
                                line++
                            }
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
                    else ->
                        if (lenient) {
                            if (! c.isJavaIdentifierPart()) {
                                expectName = false
                                break@loop
                            } else {
                                currentValue.append(c)
                                c = nextChar()
                            }
                        } else {
                            currentValue.append(c)
                        }
                }
            } while (true)

            jsonValue = currentValue.toString()
        } else if ('{' == c) {
            tokenType = TokenType.LEFT_BRACE
            expectName = true
        } else if ('}' == c) {
            tokenType = TokenType.RIGHT_BRACE
            expectName = false
        } else if ('[' == c) {
            tokenType = TokenType.LEFT_BRACKET
            expectName = false
        } else if (']' == c) {
            tokenType = TokenType.RIGHT_BRACKET
            expectName = false
        } else if (':' == c) {
            tokenType = TokenType.COLON
            expectName = false
        } else if (',' == c) {
            tokenType = TokenType.COMMA
            expectName = true
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
                throw KlaxonException("Unexpected character at position ${index-1}"
                    + ": '$c' (ASCII: ${c.toInt()})'")
            }

            tokenType = TokenType.VALUE
        } else {
            tokenType = TokenType.EOF
        }

        return Token(tokenType, jsonValue)
    }

    private fun log(s: String) = if (Debug.verbose) println(s) else ""
}
