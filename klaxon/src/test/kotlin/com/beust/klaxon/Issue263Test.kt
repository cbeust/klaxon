package com.beust.klaxon

import kotlin.reflect.KClass

fun main(arg: Array<String>) {
    val str = """
        {
            "t": null,
            "op": 10,
            "d": { "heartbeat_interval": 41250 }
        }
    """.trimIndent()

    Klaxon().parse<GatewayResponsePayload>(str)?.let { payload ->
        println(payload) // will show deserialised object, but with generic GatewayResponse object instead of ServerHello.
        if (payload.data !is ServerHello) throw RuntimeException("FAILED")
        println(payload.data is ServerHello) // will be false
    }
}
data class GatewayResponsePayload(
        @TypeFor(field = "data", adapter = DataTypeAdapter::class)
        @Json("op") val opCode: Int?,
        @Json("t") val type: String?,
        @Json("d") val data: GatewayResponse
)

open class GatewayResponse

data class ServerHello(@Json("heartbeat_interval") val heartbeatInterval: Long) : GatewayResponse()

class DataTypeAdapter : TypeAdapter<GatewayResponse> {
    override fun classFor(type: Any): KClass<out GatewayResponse> {
        if (type as Int? == 10) return ServerHello::class
        else throw RuntimeException("No type for this opcode.")
    }
}
