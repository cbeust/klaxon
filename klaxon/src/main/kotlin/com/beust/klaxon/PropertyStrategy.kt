package com.beust.klaxon

import kotlin.reflect.KProperty

interface PropertyStrategy {
    /**
     * @return true if this property should be mapped.
     */
    fun accept(property: KProperty<*>): Boolean
}