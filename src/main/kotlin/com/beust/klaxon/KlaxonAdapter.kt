package com.beust.klaxon

interface KlaxonAdapter<JsonType, T> {
    fun fromJson(json: JsonType) : T
    fun toJson(o: T) : String
}
