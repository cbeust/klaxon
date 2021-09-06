package com.beust.klaxon

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object Render {
    tailrec fun renderValue(v: Any?, result: Appendable, prettyPrint: Boolean, canonical: Boolean, level: Int) {
        when (v) {
            is JsonBase -> v.appendJsonStringImpl(result, prettyPrint, canonical, level)
            is String -> result.renderString(v)
            is Map<*, *> -> renderValue(
                    JsonObject(v.mapKeys { it.key.toString() }.mapValues { it.value }),
                    result,
                    prettyPrint,
                    canonical,
                    level)
            is List<*> -> renderValue(JsonArray(v), result, prettyPrint, canonical, level)
            is Pair<*, *> -> renderValue(v.second, result.renderString(v.first.toString()).append(": "), prettyPrint, canonical, level)
            is Double, is Float ->
                result.append(if (canonical) decimalFormat.format(v) else v.toString())
            null -> result.append("null")
            else -> {
                // TODO - Here we are reusing Converter.toJson() logic, but loose support for prettyPrint and canonical
                result.append(Klaxon().findConverter(v).toJson(v))
            }
        }
    }

    fun renderString(s: String) = StringBuilder().renderString(s).toString()

    fun escapeString(s: String): String {
        val result = StringBuilder().apply {
            for (element in s) {
                when (element) {
                    '"' -> append("\\").append(element)
//                    '\'' -> append("\\").append(ch)
                    '\\' -> append(element).append(element)
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000c' -> append("\\f")
                    else -> {
                        if (isNotPrintableUnicode(element)) {
                            append("\\u")
                            append(Integer.toHexString(element.code).padStart(4, '0'))
                        } else {
                            append(element)
                        }
                    }
                }
            }
        }
        return result.toString()
    }

    private fun <A : Appendable> A.renderString(s: String): A {
        append("\"")
        append(escapeString(s))
        append("\"")
        return this
    }

    private fun isNotPrintableUnicode(c: Char): Boolean =
            c in '\u0000'..'\u001F' ||
                    c in '\u007F'..'\u009F' ||
                    c in '\u2000'..'\u20FF'

    private val decimalFormat = DecimalFormat("0.0####E0;-0.0####E0").apply {
        decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH)
    }
}

