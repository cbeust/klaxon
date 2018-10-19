package com.beust.klaxon

/**
 * Modify field names algorithmically, both from and to JSON.
 */
interface FieldRenamer {
    fun toJson(fieldName: String) : String
    fun fromJson(fieldName: String) : String

    /**
     * Helper functions for common use cases.
     */
    companion object {
        /**
         * Takes a camel cased identifier name and returns an underscore separated
         * name
         *
         * Example:
         *     camelToUnderscores("thisIsA1Test") == "this_is_a_1_test"
         */
        fun camelToUnderscores(name: String) = "[A-Z\\d]".toRegex().replace(name) {
            "_" + it.groupValues[0].toLowerCase()
        }

        /*
         * Takes an underscore separated identifier name and returns a camel cased one
         *
         * Example:
         *    underscoreToCamel("this_is_a_1_test") == "thisIsA1Test"
         */
        fun underscoreToCamel(name: String) = "_([a-z\\d])".toRegex().replace(name) {
            it.groupValues[1].toUpperCase()

        }
    }
}
