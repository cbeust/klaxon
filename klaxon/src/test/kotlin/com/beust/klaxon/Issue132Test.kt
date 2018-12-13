package com.beust.klaxon

import org.testng.annotations.Test

@Test
class Issue132Test {
    @Test(expectedExceptions = [(KlaxonException::class)])
    fun recursion() {

        class KNode(val next: KNode)

        val converter = object : Converter {
            override fun canConvert(cls: Class<*>): Boolean = cls == Node::class.java

            override fun toJson(value: Any): String {
                return "string"
            }

            override fun fromJson(jv: JsonValue): Any {
                return ""
            }
        }

        Klaxon().converter(converter).parse<KNode>("{}")

    }
}