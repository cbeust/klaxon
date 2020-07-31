package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
class PropertyOrderTest {
    /**
     * Order the list based on the value ordering specified in order. The result string needs to contain
     * the values specified in order in the same order, while values in list but not in order will be
     * returned at the end of the list, in the same order they were in list.
     *
     * For example, "abcdef" ordered with "df" will produce "dfabce".
     */
    private fun orderWithKeys(list: List<String>, order: List<String>): List<String> {
        val comparator = Comparator<String> { o1, o2 ->
            val index1 = order.indexOf(o1)
            // If the current value is not specified in the order, it stays where it is.
            if (index1 == -1) 1
            else {
                val index2 = order.indexOf(o2)
                // The current value is found in the order, compare its index to the second value
                // Either it's not found, or it's already in the right order: stay where it is
                if (index2 == -1 || index1 < index2) -1
                // Or it's found after in the order, swap
                else 1
            }
        }
        return list.sortedWith(comparator)
    }

    @DataProvider
    fun dp() = arrayOf(
            arrayOf(listOf("a", "b"), listOf("a", "b"), listOf("a", "b")),
            arrayOf(listOf("a", "b"), listOf("b", "a"), listOf("b", "a")),
            arrayOf(listOf("a", "b", "c"), listOf("a", "b"), listOf("a", "b", "c")),
            arrayOf(listOf("a", "b", "c"), listOf("b", "a"), listOf("b", "a", "c")),

            arrayOf(listOf("a", "b", "c", "d", "e", "f"), listOf("f", "e", "d"), listOf("f", "e", "d", "a", "b", "c")),
            arrayOf(listOf("a", "c", "e", "d", "f", "b"), listOf("f", "e", "d"), listOf("f", "e", "d", "a", "c", "b"))
    )


    @Test(dataProvider = "dp")
    fun testOrderWithKeys(list: List<String>, order: List<String>, expected: List<String>) {
        println("=== Test case: $list $order")
        val result = orderWithKeys(list, order)
        assertThat(result).isEqualTo(expected)
    }

    fun testOrderWithIndex() {
        class Data(@Json(index = 1) val id: String,
                @Json(index = 2) val name: String)
        Klaxon().toJsonString(Data("id", "foo")).let {
            assertThat(it.indexOf("id")).isLessThan(it.indexOf("name"))
        }

        class Data2(@Json(index = 3) val id: String,
                @Json(index = 2) val name: String)
        Klaxon().toJsonString(Data2("id", "foo")).let {
            assertThat(it.indexOf("name")).isLessThan(it.indexOf("id"))
        }
    }
}