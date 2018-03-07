package com.beust.klaxon

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Json(
    /**
     * Used to map Kotlin properties and JSON fields that have different names.
     */
    val name: String = "",

    /**
     * If true, the property will be ignored by Klaxon.
     */
    val ignored: Boolean = false,

    /**
     * Work in progress
     */
    val path: String = ""
)
