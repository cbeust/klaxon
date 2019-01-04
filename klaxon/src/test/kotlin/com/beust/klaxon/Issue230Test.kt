package com.beust.klaxon

import org.assertj.core.api.Assertions
import org.testng.annotations.Test
import kotlin.reflect.KClass

@Test
class Issue230Test {
    enum class Setting {
        MonitoringTime,
        Threshold,
    }

    @TypeFor(field = "type", adapter = SettingValueAdapter::class)
    open class SettingValue(val type: Setting)
    data class MonitoringTime(val value: Int) : SettingValue(Setting.MonitoringTime)
    data class Threshold(val value: Double) : SettingValue(Setting.Threshold)

    data class Data(val threshold: Threshold, val monitoringTime: MonitoringTime)

    class SettingValueAdapter : TypeAdapter<SettingValue> {
        override fun classFor(type: Any): KClass<out SettingValue> = when (type as Setting) {
            Setting.MonitoringTime -> MonitoringTime::class
            Setting.Threshold -> Threshold::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    fun issue230() {
        val json = """
        {
            "threshold": { "type": "Threshold", "value":0.4 },
            "monitoringTime" : {"type": "MonitoringTime", "value":4}
        }"""

        val r = Klaxon().parse<Data>(json)
        Assertions.assertThat(r!!.threshold.value).isEqualTo(0.4)
        Assertions.assertThat(r.monitoringTime.value).isEqualTo(4)
    }
}