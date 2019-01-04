package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.reflect.KClass

@Test
class Issue229Test {
    @TypeFor(field = "type", adapter = SettingValueAdapter::class)
    open class SettingValue(val type: String)
    data class MonitoringTime(val value: Int) : SettingValue("MonitoringTime")
    data class Threshold(val value: Double) : SettingValue("Threshold")

    class SettingValueAdapter : TypeAdapter<SettingValue> {
        override fun classFor(type: Any): KClass<out SettingValue> = when (type as String) {
            "MonitoringTime" -> MonitoringTime::class
            "Threshold" -> Threshold::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    fun issue229() {
        val r = Klaxon().parse<SettingValue>("""
            {"type":"Threshold","value":0.4}
        """.trimIndent())
        assertThat(r!!.type).isEqualTo("Threshold")
        assertThat((r as Threshold).value).isEqualTo(0.4)
    }
}