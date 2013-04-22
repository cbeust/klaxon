package com.beust.klaxon

import java.io.File
import java.util.regex.Pattern

public enum class Type {
    VALUE;
    LEFT_BRACE;
    RIGHT_BRACE;
    LEFT_BRACKET;
    RIGHT_BRACKET;
    COMMA;
    COLON;
    END;
}

class Token(val tokenType: Type, val value: JsonObject?) {
    fun toString() : String {
        val v = if (value != null) { " (" + value + ")" } else {""}
        val result = tokenType.toString() + v
        return result
    }
}

public class Lexer(val fileName: String) {
    val bytes = File(fileName).readBytes()
    val END = Token(Type.END, null)
    var index = 0

    val LONG = Pattern.compile("[-]?[0-9]+")
    val DOUBLE = Pattern.compile(LONG.toString() + "((\\.[0-9]+)?([eE][-+]?[0-9]+)?)")

    fun isSpace(c: Char): Boolean {
        return c == ' ' || c == '\r' || c == '\n' || c == '\t'
    }

    private fun nextChar() : Char {
        return bytes[index++].toChar()
    }

    private fun peekChar() : Char {
        return bytes[index].toChar()
    }

    private fun isDone() : Boolean {
        return index >= bytes.size
    }

    val BOOLEAN_LETTERS = hashSetOf('f', 'a', 'l', 's', 'e', 't', 'r', 'u')
    private fun isBooleanLetter(c: Char) : Boolean {
        return BOOLEAN_LETTERS.contains(Character.toLowerCase(c))
    }

    fun isValueLetter(c: Char) : Boolean {
        return c == '-' || c == '+' || c == '.' || c.isDigit() || isBooleanLetter(c)
    }

    fun nextToken() : Token {

        if (isDone()) {
            return END
        }

        var tokenType = Type.END
        var c = nextChar()
        var currentValue = StringBuilder()
        var jsonValue: JsonObject? = null

        while (! isDone() && isSpace(c)) {
            c = nextChar()
        }

        if ('"' == c) {
            tokenType = Type.VALUE
            if (! isDone()) {
                c = nextChar()
                while (index < bytes.size && c != '"') {
                    currentValue.append(c)
                    if (c == '\\' && index < bytes.size) {
                        c = nextChar()
                        currentValue.append(c)
                    }
                    c = nextChar()
                }
                jsonValue = JsonString(currentValue.toString())
            } else {
                throw RuntimeException("Unterminated string")
            }
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
                    break;
                } else {
                    c = nextChar()
                }
            }
            val v = currentValue.toString()
            if (LONG.matcher(v).matches()) {
                jsonValue = JsonLong(java.lang.Long.parseLong(v))
            } else if (DOUBLE.matcher(v).matches()) {
                jsonValue = JsonDouble(java.lang.Double.parseDouble(v))
            } else if ("true".equals(v.toLowerCase())) {
                jsonValue = JsonBoolean(true)
            } else if ("false".equals(v.toLowerCase())) {
                jsonValue = JsonBoolean(false)
            }
            tokenType = Type.VALUE
        }

        val value = currentValue.toString()
        return Token(tokenType, if (value.length() > 0) jsonValue else null)
    }
}
