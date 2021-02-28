package com.beust.klaxon

// While this is a valid JSON key name, it is unlikely to actually be used.
const val NAME_NOT_INITIALIZED = "Klaxon:This field was not initialized!@#$%^&*()_+AIS8X9A4NT"

// Commented VALUE_PARAMETER because of https://youtrack.jetbrains.com/issue/KT-23229
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR
    // AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Json(
    /**
     * Used to map Kotlin properties and JSON fields that have different names.
     */
    val name: String = NAME_NOT_INITIALIZED,

    /**
     * If true, the property will be ignored by Klaxon.
     */
    val ignored: Boolean = false,

    /**
     * Work in progress
     */
    val path: String = "",

    /**
     * Where this property should appear in the JSON output. Lower numbers appear first.
     */
    val index: Long = Long.MAX_VALUE,

    /**
     * If false, property will be absent in JSON when value is null.
     * If true, property will be present in JSON with value null.
     */
    val serializeNull: Boolean = true
)

fun Json.nameInitialized() = this.name != NAME_NOT_INITIALIZED