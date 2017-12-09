package com.beust.klaxon

import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import kotlin.reflect.KClass

class Klaxon {
    val jsonAdapter = JsonAdapter()

    fun parse(fileName: String) = Parser().parse(fileName)

    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8) = Parser().parse(inputStream, charset)

    fun parse(reader: Reader) = Parser().parse(reader)

    fun typeAdapter(annotation: KClass<out Annotation>, adapter: KlaxonAdapter<*>): Klaxon {
        jsonAdapter.typeAdapter(annotation, adapter)
        return this
    }

    inline fun <reified T> fromJson(json: String) : T? = jsonAdapter.fromJson<T>(json)
}
