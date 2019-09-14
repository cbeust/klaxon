package com.beust.klaxon

interface PropertyStrategy {
    /**
     * @return true if this property should be mapped.
     */
    fun accept(property: Property1): Boolean
}