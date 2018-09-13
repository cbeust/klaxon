package com.beust.klaxon

open class KlaxonException(s: String) : RuntimeException(s)
class JsonParsingException(s: String) : KlaxonException(s)