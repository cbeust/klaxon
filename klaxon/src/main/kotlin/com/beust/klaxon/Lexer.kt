package com.beust.klaxon

import com.beust.klaxon.token.*
import java.io.Reader
import java.util.regex.Pattern

/**
 * if `lenient` is true, names (the identifiers left of the colon) are allowed to not be surrounded by double quotes.
 */
class Lexer(passedReader: Reader, val lenient: Boolean = false): Iterator<Token> {
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
    private val isDone: Boolean
        get() = next == null

    init {
        val c = reader.read()
        next = if (c == -1) null else c.toChar()
    }

    private fun nextChar(): Char {
        if (isDone) throw IllegalStateException("Cannot get next char: EOF reached")
        val c = next!!
        next = reader.read().let { if (it == -1) null else it.toChar() }
        index++
//        log("Next char: '$c' index:$index")
        return c
    }

    private fun peekChar() : Char {
        if (isDone) throw IllegalStateException("Cannot peek next char: EOF reached")
        return next!!
    }

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
        peeked = peeked ?: actualNextToken()
        return peeked!!
    }

    override fun next() = nextToken()
    override fun hasNext() = peek() !is EOF

    fun nextToken(): Token {
        return peeked?.also { peeked = null } ?: actualNextToken()
    }

    private var expectName = false

    private fun actualNextToken() : Token {

        if (isDone) {
            return EOF
        }

        val token: Token
        var c = nextChar()
        val currentValue = StringBuilder()

        while (!isDone && isSpace(c)) {
            c = nextChar()
        }

        if ('"' == c || (lenient && expectName)) {
            if (lenient) {
                currentValue.append(c)
            }
            loop@
            do {
                if (isDone) {
                    throw KlaxonException("Unterminated string")
                }

                c = if (lenient) peekChar() else nextChar()
                when (c) {
                    '\\' -> {
                        if (isDone) {
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
                                nextChar()
                            }
                        } else {
                            currentValue.append(c)
                        }
                }
            } while (true)

            token = Value(currentValue.toString())
        } else if ('{' == c) {
            token = LEFT_BRACE
            expectName = true
        } else if ('}' == c) {
            token = RIGHT_BRACE
            expectName = false
        } else if ('[' == c) {
            token = LEFT_BRACKET
            expectName = false
        } else if (']' == c) {
            token = RIGHT_BRACKET
            expectName = false
        } else if (':' == c) {
            token = COLON
            expectName = false
        } else if (',' == c) {
            token = COMMA
            expectName = true
        } else if (!isDone) {
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
                token = try {
                    Value(java.lang.Integer.parseInt(v))
                } catch (e: NumberFormatException){
                    try {
                        Value(java.lang.Long.parseLong(v))
                    } catch(e: NumberFormatException) {
                        Value(java.math.BigInteger(v))
                    }
                }
            } else if (DOUBLE.matcher(v).matches()) {
                token = Value(java.lang.Double.parseDouble(v))
            } else if ("true" == v.toLowerCase()) {
                token = Value(true)
            } else if ("false" == v.toLowerCase()) {
                token = Value(false)
            } else if (v == "null") {
                token = Value(null)
            } else {
                throw KlaxonException("Unexpected character at position ${index-1}"
                        + ": '$c' (ASCII: ${c.toInt()})'")
            }

        } else {
            token = EOF
        }

        return token
    }


    private fun log(s: String) { if (Debug.verbose) println(s) }
}
