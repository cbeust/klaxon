package com.beust.klaxon

interface JsonBase {
    fun appendJsonStringImpl(result: Appendable, prettyPrint: Boolean, canonical: Boolean, level: Int)
    fun appendJsonString(result : Appendable, prettyPrint: Boolean = false, canonical: Boolean = false) =
            appendJsonStringImpl(result, prettyPrint, canonical, 0)
    fun toJsonString(prettyPrint: Boolean = false, canonical: Boolean = false) : String =
            StringBuilder().apply { appendJsonString(this, prettyPrint, canonical) }.toString()
}