package com.beust.klaxon

/**
 * Invoked whenever the JSON Path encountered matches as a regexp the given path.
 */
interface PathObserver {
    fun onMatch(path: String, value: Any)
}
