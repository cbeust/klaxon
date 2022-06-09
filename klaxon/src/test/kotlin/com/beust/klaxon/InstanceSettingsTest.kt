package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

@Test
class InstanceSettingsTest {

    @Suppress("unused")
    class UnannotatedGeolocationCoordinates(
        val latitude: Int,
        val longitude: Int,
        val speed: Int? // nullable field
    )

    @Suppress("unused")
    class NoNullAnnotatedGeolocationCoordinates(
        val latitude: Int,
        val longitude: Int,
        @Json(serializeNull = false) val speed: Int? // nullable field
    )

    @Suppress("unused")
    class NullAnnotatedGeolocationCoordinates(
        val latitude: Int,
        val longitude: Int,
        @Json(serializeNull = true) val speed: Int? // nullable field
    )

    private val unannotatedCoordinates = UnannotatedGeolocationCoordinates(1, 2, null)
    private val noNullCoordinates = NoNullAnnotatedGeolocationCoordinates(1, 2, null)
    private val nullCoordinates = NullAnnotatedGeolocationCoordinates(1, 2, null)

    // Defaults & single-type settings

    @Test
    fun defaultSerialization() {
        val klaxon = Klaxon()
        val json = klaxon.toJsonString(unannotatedCoordinates)
        assertContains(json, "null") // {"latitude" : 1, "longitude" : 2, "speed" : null}
    }

    @Test // no local settings, instance serializeNull = true -> null
    fun instanceSettingsNullSerialization() {
        val klaxon = Klaxon(instanceSettings = KlaxonSettings(serializeNull = true))
        val json = klaxon.toJsonString(unannotatedCoordinates)
        assertContains(json, "null") // {"latitude" : 1, "longitude" : 2, "speed" : null}
    }

    @Test // no local settings, instance serializeNull = false -> no null
    fun instanceSettingsNoNullSerialization() {
        val klaxon = Klaxon(KlaxonSettings(serializeNull = false))
        val json = klaxon.toJsonString(unannotatedCoordinates)
        assertFalse { json.contains("null") } // {"latitude" : 1, "longitude" : 2}
    }

    @Test // local serializeNull = false, no instance settings -> no null
    fun localSettingsNoNullSerialization() {
        val klaxon = Klaxon()
        val json = klaxon.toJsonString(noNullCoordinates)
        assertFalse { json.contains("null") } // {"latitude" : 1, "longitude" : 2}
    }

    @Test // local serializeNull = true, no instance settings -> null
    fun localSettingsNullSerialization() {
        val klaxon = Klaxon()
        val json = klaxon.toJsonString(nullCoordinates)
        assertContains(json, "null") // {"latitude" : 1, "longitude" : 2, "speed" : null}
    }

    //
    // Mixed tests

    @Test // local serializeNull = true, instance serializeNull = false -> null
    fun localNullInstanceNoNullSerialization() {
        val klaxon = Klaxon(KlaxonSettings(serializeNull = false))
        val json = klaxon.toJsonString(nullCoordinates)
        assertContains(json, "null") // {"latitude" : 1, "longitude" : 2, "speed" : null}
    }

    @Test // local serializeNull = false, instance serializeNull = true -> no null
    fun localNoNullInstanceNullSerialization() {
        val klaxon = Klaxon(KlaxonSettings(serializeNull = true))
        val json = klaxon.toJsonString(noNullCoordinates)
        assertFalse { json.contains("null") } // {"latitude" : 1, "longitude" : 2}
    }
}
