package com.beust.klaxon

fun valueToString(v: Any?, prettyPrint: Boolean = false, canonical : Boolean = false) : String =
    StringBuilder().apply {
        Render.renderValue(v, this, prettyPrint, canonical, 0)
    }.toString()

fun Appendable.indent(level: Int) {
    for (i in 1..level) {
        append("  ")
    }
}

