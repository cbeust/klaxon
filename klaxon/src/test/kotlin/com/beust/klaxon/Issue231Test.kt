package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.reflect.KClass

@Test
class Issue231Test {
    @TypeFor(field = "type", adapter = SettingValueAdapter::class)
    open class SettingValue(val type: String)
    data class MonitoringTime(val value: Int) : SettingValue("MonitoringTime")
    data class Threshold(val value: Double) : SettingValue("Threshold")

    data class Data(val threshold: Threshold, val monitoringTime: MonitoringTime)

    class SettingValueAdapter : TypeAdapter<SettingValue> {
        override fun classFor(type: Any): KClass<out SettingValue> = when (type as String) {
            "MonitoringTime" -> MonitoringTime::class
            "Threshold" -> Threshold::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    fun issue231() {
        val json = """
        {
            "threshold": { "type": "Threshold", "value":0.4 },
            "monitoringTime" : {"type": "MonitoringTime", "value":4}
        }"""

        val r = Klaxon().parse<Data>(json)
        assertThat(r!!.threshold.value).isEqualTo(0.4)
        assertThat(r.monitoringTime.value).isEqualTo(4)
    }
}