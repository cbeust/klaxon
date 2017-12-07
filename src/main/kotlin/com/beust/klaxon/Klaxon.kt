package com.beust.klaxon

import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

class Klaxon {
    fun parse(fileName: String) = Parser().parse(fileName)

    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8) = Parser().parse(inputStream, charset)

    fun parse(reader: Reader) = Parser().parse(reader)

    fun <T> adapter() : JsonAdapter {
        return JsonAdapter()
    }
}
