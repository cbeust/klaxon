package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class KlaxonTest2 {

    enum class MessageType {
        RECEIVED, SENDING
    }

    sealed class Message(open val text: String,
            @Json(name = "message_type")
            val messageType: MessageType) {

        data class MessageReceived(val sender: String,
                override val text: String) : Message(text, MessageType.RECEIVED)

        data class MessageSending(val recipient: String,
                override val text: String) : Message(text, MessageType.SENDING)
    }

    @Test
    fun testEncoding() {
        val klaxon = Klaxon()
        val received = Message.MessageReceived("Alice" , "Hello")
        val receivedString = klaxon.toJsonString(received)
        assertThat(receivedString).contains("message_type")
    }
}

class KlaxonTest3 {

    enum class MessageType {
        RECEIVED, SENDING
    }

    abstract class Message(open val text: String,
            @Json(name = "message_type")
            val messageType: MessageType)

    data class MessageReceived(val sender: String,
            override val text: String) : Message(text, MessageType.RECEIVED)

    data class MessageSending(val recipient: String,
            override val text: String) : Message(text, MessageType.SENDING)

    @Test
    fun testEncoding() {
        val klaxon = Klaxon()
        val received = MessageReceived("Alice", "Hello")
        val receivedString = klaxon.toJsonString(received)
        assertThat(receivedString).contains("message_type")
    }
}