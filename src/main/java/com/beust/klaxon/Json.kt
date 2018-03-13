package com.beust.klaxon

// Commented VALUE_PARAMETER because of https://youtrack.jetbrains.com/issue/KT-23229
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR
    // AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
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
