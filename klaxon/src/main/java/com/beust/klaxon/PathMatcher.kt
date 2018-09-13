package com.beust.klaxon

/**
 * Invoked whenever the JSON Path encountered matches as a regexp the given path.
 */
interface PathMatcher {
    /**
     * @return true if we want to observe this path.
     */
    fun pathMatches(path: String) : Boolean

    /**
     * Note that the value can only be a basic type (Int, String, ...) and never JsonObject or JsonArray.
     */
    fun onMatch(path: String, value: Any)
}
