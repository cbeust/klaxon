package com.beust.klaxon

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

interface Parser {

    /**
     * Parse the json from a raw string contained in the [StringBuilder].
     */
    fun parse(rawValue: StringBuilder): Any

    /**
     * Parse the json from a file with the given [fileName].
     * @return a JsonObject or JsonArray
     */
    fun parse(fileName: String) : Any =
        FileInputStream(File(fileName)).use {
            parse(it)
        }

    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8): Any

    fun parse(reader: Reader): Any

    /**
     * Provides access to instances of the [Parser].
     * Plugins will add additional parsers by adding extension functions to this companion object.
     */
    companion object {

        @Deprecated(
            message = "Please use a factory method to create the Parser.",
            replaceWith = ReplaceWith(expression = "default(pathMatchers, passedLexer, streaming)")
        )
        operator fun invoke(
            pathMatchers: List<PathMatcher> = emptyList(),
            passedLexer: Lexer? = null,
            streaming: Boolean = false
        ): Parser =
            KlaxonParser(
                pathMatchers,
                passedLexer,
                streaming
            )

        /**
         * Main entry for Klaxon's parser.
         *
         * If [streaming] is true, the parser doesn't expect to finish on an EOF token. This is used for streaming, when
         * the user requests to read a subset of the entire JSON document.
         */
        fun default(
            pathMatchers: List<PathMatcher> = emptyList(),
            passedLexer: Lexer? = null,
            streaming: Boolean = false
        ): Parser = KlaxonParser(
            pathMatchers,
            passedLexer,
            streaming
        )
    }
}
