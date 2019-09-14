package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.reflect.KClass

@Test
class IssuesTest {
    class SensorTypeAdapter : TypeAdapter<Sensor> {
        override fun classFor(type: Any): KClass<out Sensor> =
                when (Sensor.Type.valueOf(type as String)) {
                    Sensor.Type.Test -> Test::class
                    else -> throw IllegalArgumentException("Unknown sensor type $type")
                }
    }

    class Test(val prop: String) : Sensor(Type.Test)

    @TypeFor(field = "type", adapter = SensorTypeAdapter::class)
    abstract class Sensor(val type: Type) {
        enum class Type { T1, T2, Test }
    }


    fun issue243() {
        val json = """
            {
                "type": "Test",
                "prop": "String"
            }
                    """.trimIndent()
        val result = Klaxon().parse<Sensor>(json)
        assertThat(result!!.type.name).isEqualTo("Test")
    }
}

class SensorTypeAdapter : TypeAdapter<Sensor> {
    override fun classFor(type: Any): KClass<out Sensor> =
            when (Sensor.Type.valueOf(type as String)) {
                Sensor.Type.Test -> ThisTest::class
                else -> throw IllegalArgumentException("Unknown sensor type $type")
            }
}

data class ThisTest(val prop: String) : Sensor(Type.Test)

@TypeFor(field = "type", adapter = SensorTypeAdapter::class)
abstract class Sensor(val type: Type) {
    enum class Type { T1, T2, Test }
}


fun main(arg: Array<String>) {
    val json = """
            {
                "type": "Test",
                "prop": "String"
            }"""
    val result = Klaxon().parse<Sensor>(json)
    println(result)
}

