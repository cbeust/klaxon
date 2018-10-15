package com.beust.klaxon

import com.beust.klaxon.token.Token
import com.beust.klaxon.token.TokenType

private data class TokenStatus(val status: Status, val tokenType: TokenType)

class StateMachine(private val streaming: Boolean) {
    private val map = hashMapOf<TokenStatus, (world: World, token: Token) -> World>()

    fun put(status: Status, tokenType: TokenType, processor: (world: World, token: Token) -> World) {
        map[TokenStatus(status, tokenType)] = processor
    }

    fun next(world: World, token: Token) : World {
        val pair = TokenStatus(world.status, token.tokenType)
        val processor = map[pair]

        return if (processor != null) {
            processor(world, token)
        } else {
            if (!streaming) {
                fun formatList(l: List<String>) : String {
                    val result = StringBuilder()
                    l.withIndex().forEach { iv ->
                        if (iv.index == l.size - 1) result.append(" or ")
                        else if (iv.index > 0) result.append(", ")
                        result.append(iv.value)
                    }
                    return result.toString()
                }

                val validTokens = map.keys.filter { it.status == world.status }.map { it.tokenType.toString() }
                val validTokenMessage =
                        if (validTokens.size == 1) validTokens[0]
                        else formatList(validTokens)

                val message = "Expected $validTokenMessage, not '$token' at line ${world
                        .line}" +
                    "\n   (internal error: \"No processor found for: (${world.status}, $token)\""
                throw KlaxonException(message)
            } else {
                World(Status.EOF)
            }
        }
    }
}