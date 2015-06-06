package com.beust.klaxon

private fun <A: Appendable> A.renderString(s: String): A {
    append("\"")

    for (idx in 0..s.length() - 1) {
        val ch = s[idx]
        when (ch) {
            '"' -> append("\\").append(ch)
            '\\' -> append(ch).append(ch)
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(ch)
        }
    }

    append("\"")
    return this
}

private fun renderValue(v: Any?, result: Appendable, prettyPrint: Boolean, level: Int) {
    when (v) {
        is JsonBase -> v.appendJsonStringImpl(result, prettyPrint, level)
        is String -> result.renderString(v)
        is Map<*, *> -> renderValue(JsonObject(v.mapKeys { it.key.toString() }.mapValues { it.value?.toString() }), result, prettyPrint, level)
        is List<*> -> renderValue(JsonArray(v.map { it?.toString() }), result, prettyPrint, level)
        else -> result.append(v.toString())
    }
}

private fun Appendable.indent(level: Int) {
    for (i in 1..level) {
        append("  ")
    }
}

