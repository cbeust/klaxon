package com.beust.klaxon

import org.testng.annotations.Test

data class MeasurementConditionDouble(val name: String, val min: Double, val max: Double)
data class MeasurementConditionFloat(val name: String, val min: Float, val max: Float)

val measurementConditionDouble = MeasurementConditionDouble("foo", 1.0,2.0)
val measurementConditionFloat = MeasurementConditionFloat("foo", 1.0f, 2.0f)

@Test
class Issue198Test {
    fun f() {
        Klaxon().toJsonString(measurementConditionDouble)
        Klaxon().toJsonString(measurementConditionFloat)
    }
}
