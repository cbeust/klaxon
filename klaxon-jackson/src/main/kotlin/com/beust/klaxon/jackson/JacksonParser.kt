package com.beust.klaxon.jackson

import com.beust.klaxon.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeType
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import java.util.Base64

/**
 * An implementation of the [Parser] that uses
 * [Jackson](https://github.com/FasterXML/jackson).
 *
 * On very large JSON payloads this parser has been found to take 1/2 the time of the klaxon [Parser].
 */
fun Parser.Companion.jackson(): Parser =
    JacksonParser.create()

internal class JacksonParser
private constructor(
    private val mapper: ObjectMapper
) : Parser {
    override fun parse(rawValue: StringBuilder): Any {
        return mapper.readValue(rawValue.toString(), Any::class.java)
    }

    override fun parse(inputStream: InputStream, charset: Charset): Any {
        // charset is ignored
        return mapper.readValue(inputStream, Any::class.java)
    }

    override fun parse(reader: Reader): Any {
        return mapper.readValue(reader, Any::class.java)
    }

    companion object {
        fun create(): JacksonParser {
            val mapper = ObjectMapper().apply {
                registerModule(SimpleModule().apply {
                    addDeserializer(Map::class.java, KlaxonJsonObjectDeserializer())
                    addDeserializer(List::class.java, KlaxonJsonArrayDeserializer())
                })
            }
            return JacksonParser(mapper)
        }
    }
}

internal class KlaxonJsonObjectDeserializer
internal constructor() : JsonDeserializer<JsonObject>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JsonObject {
        val node: JsonNode = p.codec.readTree(p)
        return parseJsonObject(node)
    }
}

internal class KlaxonJsonArrayDeserializer
internal constructor() : JsonDeserializer<JsonArray<*>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JsonArray<*> {
        val node: JsonNode = p.codec.readTree(p)
        return parseJsonArray(node)
    }
}

internal fun parseJsonObject(node: JsonNode) = json {
    parseJsonObject(node)
}

internal fun parseJsonArray(node: JsonNode) = json {
    parseJsonArray(node)
}

private fun KlaxonJson.parseJsonObject(node: JsonNode): JsonObject {
    val pairs =
        node
            .fields()
            .asSequence()
            .map { it.key to parseValue(it.value) }
            .toList()
    return obj(pairs)
}

private fun KlaxonJson.parseJsonArray(node: JsonNode): JsonArray<*> {
    val sequence =
        node
            .elements()
            .asSequence()
            .map { parseValue(it) }
    return array(sequence.toList())
}

private fun KlaxonJson.parseValue(node: JsonNode): Any? {
    val nodeType = checkNotNull(node.nodeType) {
        "JsonNode.nodeType was null."
    }
    return when (nodeType) {
        JsonNodeType.STRING -> node.asText()
        JsonNodeType.OBJECT -> parseJsonObject(node)
        JsonNodeType.ARRAY -> parseJsonArray(node)
        JsonNodeType.BOOLEAN -> node.asBoolean()
        JsonNodeType.NUMBER -> {
            when {
                node.isInt -> node.asInt()
                node.isLong -> node.asLong()
                node.isDouble -> node.doubleValue()
                node.isFloat -> node.floatValue()
                node.isBigDecimal -> node.decimalValue()
                node.isBigInteger -> node.bigIntegerValue()
                else ->
                    throw UnsupportedOperationException("Unsupported number type: $node")
            }
        }
        JsonNodeType.BINARY -> Base64.getEncoder().encodeToString(node.binaryValue())
        JsonNodeType.NULL -> null
        JsonNodeType.MISSING -> throw KlaxonException("Unexpected node type: $nodeType")
        JsonNodeType.POJO ->
            throw UnsupportedOperationException("Unsupported type ${JsonNodeType.POJO}")
    }
}
