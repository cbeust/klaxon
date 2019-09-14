package com.beust.klaxon

import org.testng.annotations.Test
import kotlin.reflect.full.memberProperties
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    MiscTest().issue154()
}

@Test
class MiscTest {

    fun issue154() {
        class ErrorMessageData(val message: String)
        val r = measureTimeMillis {
            ErrorMessageData::class.memberProperties
        }
        println("Time: $r ms")
    }
}