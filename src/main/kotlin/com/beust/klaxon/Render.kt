package com.beust.klaxon

private fun <A: Appendable> A.renderString(s: String): A {
    append("\"")

    for (idx in 0..s.length - 1) {
        val ch = s[idx]
        when (ch) {
            '"' -> append("\\").append(ch)
            '\\' -> append(ch).append(ch)
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000c' -> append("\\f")
            else -> {
                if(isPrintableUnicode(ch)){
                    append("\\u")
                    append(Integer.toHexString(ch.toInt()).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
    }

    append("\"")
    return this
}

private fun isPrintableUnicode(c: Char) : Boolean = ((c >= '\u0000' && c <= '\u001F')
        || (c >= '\u007F' && c <= '\u009F') || (c >= '\u2000' && c <= '\u20FF'))

tailrec fun renderValue(v: Any?, result: Appendable, prettyPrint: Boolean, level: Int) {
    when (v) {
        is JsonBase -> v.appendJsonStringImpl(result, prettyPrint, level)
        is String -> result.renderString(v)
        is Map<*, *> -> renderValue(
                JsonObject(v.mapKeys { it.key.toString() }.mapValues { it.value }),
                result,
                prettyPrint,
                level)
        is List<*> -> renderValue(JsonArray(v), result, prettyPrint, level)
        is Pair<*, *> -> renderValue(v.second, result.renderString(v.first.toString()).append(": "), prettyPrint, level)
        else -> result.append(v.toString())
    }
}

fun Appendable.indent(level: Int) {
    for (i in 1..level) {
        append("  ")
    }
}

