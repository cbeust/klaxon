package com.beust.klaxon

data class KlaxonSettings(

    /**
     * If false, property will be absent in JSON when value is null.
     * If true, property will be present in JSON with value null.
     */
    val serializeNull: Boolean = true
)
