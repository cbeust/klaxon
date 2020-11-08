package com.beust.klaxon

import org.testng.annotations.Test
import java.lang.RuntimeException
import kotlin.reflect.KClass
import kotlin.test.assertTrue

@Test
class Issue263Test {
    data class GatewayMessage(
        @TypeFor(field = "data", adapter = GatewayPayloadTypeAdapter::class)
        @Json("op") val opCode: Int,
        @Json("t") val type: String?,
        @Json("d") val data: GatewayPayload
    )

    open class GatewayPayload

    data class ServerHello(
        @Json("heartbeat_interval") val heartbeatInterval: Long
    ): GatewayPayload()

    class GatewayPayloadTypeAdapter : TypeAdapter<GatewayPayload> {
        override fun classFor(type: Any): KClass<out GatewayPayload> = when(type as Int) {
            10 -> ServerHello::class
            else -> throw RuntimeException("No type for this opcode")
        }
    }

    fun issue263() {
        val input = """
            {
                "t": null,
                "op": 10,
                "d": {
                    "heartbeat_interval": 41250
                }
            }
        """.trimIndent()

        val parsed = Klaxon().parse<GatewayMessage>(input)!!
        assertTrue(parsed.data is ServerHello, "expected 'data' to be ${ServerHello::class}, got ${parsed.data::class}")
    }
}
