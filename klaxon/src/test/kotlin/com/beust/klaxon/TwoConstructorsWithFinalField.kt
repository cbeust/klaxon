package com.beust.klaxon

import org.testng.Assert
import org.testng.annotations.Test

@Test
class TwoConstructorsWithFinalField {

    class StringOrInt (
        val stringOrInt: String
    ) {
        constructor( stringOrInt: Int ) : this( "$stringOrInt" )
    }

    fun stringWorks() {

        val sampleJson = """{"stringOrInt":"5"}"""
        val result = Klaxon().parse<StringOrInt>(sampleJson)

        Assert.assertNotNull(result)
        Assert.assertEquals(result!!.stringOrInt, "5")
    }

    fun intWorks() {

        val sampleJson = """{"stringOrInt":5}"""
        val result = Klaxon().parse<StringOrInt>(sampleJson)

        Assert.assertNotNull(result)
        Assert.assertEquals(result!!.stringOrInt, "5")
    }

}
