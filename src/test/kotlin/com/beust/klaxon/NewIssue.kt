package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.lang.reflect.ParameterizedType
import java.util.*

data class ConferenceDataModel(
        val events: Map<String, EventModel>,
        val rooms: Map<String, RoomModel>,
        val sections: Map<String, SectionModel>,
        val speakers: Map<String, SpeakerModel>,
        val tracks: Map<String, TrackModel>
)

data class EventModel(
        val name: String,
        val description: String,
        val duration: String,
        val isGeneralEvent: Boolean,
        val isPublished: Boolean,
        val startTime: Date,
        val roomIds: Map<String, Boolean>?,
        val speakerIds: Map<String, Boolean>?,
        val trackId: String?,
        var roomNames: Map<String, Boolean>?,
        var speakerNames: Map<String, Boolean>?,
        var trackName: String?,
        val updatedAt: Long,
        val updatedBy: String
)

data class RoomModel(
        val name: String,
        val updatedAt: Long,
        val updatedBy: String
)

data class SectionModel(
        val name: String,
        val title: String,
        val startTime: Date,
        val endTime: Date,
        val updatedAt: Long,
        val updatedBy: String
)

data class SpeakerModel(
        val name: String,
        val title: String?,
        val org: String?,
        val bio: String,
        val pictureId: String?,
        val pictureUrl: String?,
        val isFeatured: Boolean,
        val socialProfiles: Map<String,String>?,
        val updatedAt: Long,
        val updatedBy: String
)

data class TrackModel(
        val name: String,
        val description: String,
        val sortOrder: Int,
        val updatedAt: Long,
        val updatedBy: String
)

data class Node(val nodeName: String)
class Root(val nodes: Map<String, Node>)

@Test
class NewIssue {
    fun f1() {
        val r = Klaxon()
            .parse<HashMap<String, Node>>("""{
                "key1": { "nodeName": "node1" },
                "key2": { "nodeName": "node2" }
            }""")
        assertThat(r!!.size).isEqualTo(2)
        assertThat(r["key1"]).isEqualTo(Node("node1"))
        assertThat(r["key2"]).isEqualTo(Node("node2"))
        println(r)
    }

    class Model(val events: Map<String, Node>)
    fun f2() {
        val r = Klaxon()
            .converter(BindingAdapterTest.CARD_ADAPTER)
//            .converter(mapConverter)
            .parse<Model>("""{
                "events": {
                    "key1": { "nodeName": "node1" },
                    "key2": { "nodeName": "node2" }
                }
            }""")
        val n1 = r!!.events["key1"]
        assertThat(r!!.events["key1"]).isEqualTo(Node("node1"))
    }

    fun f() {
        val ins = NewIssue::class.java.getResourceAsStream("/data.json")
        val r = Klaxon()
            .parse<ConferenceDataModel>(ins!!)
        println(r)
    }

    open class TypeLiteral<T> {
        val type: java.lang.reflect.Type
            get() = (javaClass.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()[0]
    }

    inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {} // here T is replaced with the actual type

    inline fun <reified T> f3() {
        val h = hashMapOf<String, T>()
        val cls = h::class
        val t = T::class
        val tl = typeLiteral<Map<String, T>>()
        val ta = (cls.java as ParameterizedType).actualTypeArguments
        println(cls)
    }

    fun reified() {
        f3<Long>()
    }
}
