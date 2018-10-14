package com.beust.klaxon.token

/**
 * Token produced by the Lexer. This construct (sealed class) allows Tokens
 * to be used both as types and as actual Token objects.
 *
 * Value Tokens are generic, so their value field doesn't need to be casted from Any?.
 * The rest of the Token objects are singletons (Kotlin 'object'),
 * which allows them to be used as types (like enums).
 */
sealed class Token<out T>(val value: T) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
        || (other is Token<*> && value == other.value)
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "${this::class.java.simpleName} (${tokenType.value})"
    }

    val tokenType: TokenType
        get() = if (this is Value) VALUE_TYPE else this
}

class Value<out T>(value: T) : Token<T>(value)
object VALUE_TYPE : Token<String>("a value")    // Use as a TokenType only
object LEFT_BRACE : Token<String>("\"{\"")
object RIGHT_BRACE : Token<String>("\"}\"")
object LEFT_BRACKET : Token<String>("\"[\"")
object RIGHT_BRACKET : Token<String>("\"]\"")
object COMMA : Token<String>("\",\"")
object COLON : Token<String>("\":\"")
object EOF : Token<String>("EOF")

// This should be used when referring to a Token as a type, to avoid confusion. (see StateMachine)
typealias TokenType = Token<*>
