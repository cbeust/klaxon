package com.beust.klaxon.token

/**
 * Token produced by the Lexer. This construct (sealed class) allows Tokens
 * to be used both as types and as actual Token objects.
 *
 * Value Tokens are generic, so their value field doesn't need to be casted from Any?.
 * The rest of the Token objects are singletons (Kotlin 'object'),
 * which allows them to be used as types (like enums).
 */
sealed class Token {
    override fun equals(other: Any?): Boolean =
        super.equals(other)
                || (this is Value<*> && other is Value<*> && this.value == other.value)

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = when(this) {
        is Value<*> -> if (value is String) "\"$value\"" else value.toString()
        is VALUE_TYPE -> "a value"
        is LEFT_BRACE -> "{"
        is RIGHT_BRACE -> "}"
        is LEFT_BRACKET -> "["
        is RIGHT_BRACKET -> "]"
        is COMMA -> ","
        is COLON -> ":"
        is EOF -> "EOF"
    }

    val tokenType: TokenType
        get() = if (this is Value<*>) VALUE_TYPE else this
}

open class Value<out T>(val value: T) : Token()
object VALUE_TYPE : Value<Nothing?>(null)   // Use as a TokenType only
object LEFT_BRACE : Token()
object RIGHT_BRACE : Token()
object LEFT_BRACKET : Token()
object RIGHT_BRACKET : Token()
object COMMA : Token()
object COLON : Token()
object EOF : Token()

// This should be used when referring to a Token as a type, to avoid confusion. (see StateMachine)
typealias TokenType = Token
