package com.beust.klaxon

import kotlin.test.assertEquals
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import kotlin.test.assertNotNull

class KlaxonTest {
    BeforeClass
    fun bc() {
    }

    Test
    fun basic() {
        val cls = javaClass<KlaxonTest>()
        val inputStream = cls.getResourceAsStream("/a.json")
        val j = Parser().parse(inputStream!!)
        assertEquals("b", j.get("a")!!.asString())
        val array = j.get("c")!!.asList()
        assertEquals(4, array.size())
        var i = 0
        assertEquals(array.get(i++)!!.asLong(), 1.toLong())
        assertEquals(array.get(i++)!!.asDouble(), 2.34)
        assertEquals(array.get(i++)!!.asString(), "abc")
        assertEquals(array.get(i++)!!.asBoolean(), false)
    }
}

