package com.beust.klaxon

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Jackson’s date formatter, pruned to Moshi's needs. Forked from this file:
 * https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/util/ISO8601Utils.java

 * Utilities methods for manipulating dates in iso8601 format. This is much much faster and GC
 * friendly than using SimpleDateFormat so highly suitable if you (un)serialize lots of date
 * objects.

 * Supported parse format: [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:]mm]]

 * @see [this specification](http://www.w3.org/TR/NOTE-datetime)
 */
internal object Iso8601Utils {
    /** ID to represent the 'GMT' string  */
    val GMT_ID = "GMT"

    /** The GMT timezone, prefetched to avoid more lookups.  */
    val TIMEZONE_Z: TimeZone = TimeZone.getTimeZone(GMT_ID)

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format
     * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]

     * @param date ISO string to parse in the appropriate format.
     * *
     * @return the parsed date
     */
    fun parse(date: String): Date {
        try {
            var offset = 0

            // extract year
            val year = parseInt(date, offset, offset + 4)
            offset += 4
            if (checkOffset(date, offset, '-')) {
                offset += 1
            }

            // extract month
            val month = parseInt(date, offset, offset + 2)
            offset += 2
            if (checkOffset(date, offset, '-')) {
                offset += 1
            }

            // extract day
            val day = parseInt(date, offset, offset + 2)
            offset += 2
            // default time value
            var hour = 0
            var minutes = 0
            var seconds = 0
            var milliseconds = 0 // always use 0 otherwise returned date will include millis of current time

            // if the value has no time component (and no time zone), we are done
            val hasT = checkOffset(date, offset, 'T')

            if (!hasT && date.length <= offset) {
                val calendar = GregorianCalendar(year, month - 1, day)

                return calendar.time
            }

            if (hasT) {

                // extract hours, minutes, seconds and milliseconds
                hour = parseInt(date, offset + 1, offset + 2)
                offset += 3
                if (checkOffset(date, offset, ':')) {
                    offset += 1
                }

                minutes = parseInt(date, offset, offset + 2)
                offset += 2
                if (checkOffset(date, offset, ':')) {
                    offset += 1
                }
                // second and milliseconds can be optional
                if (date.length > offset) {
                    val c = date[offset]
                    if (c != 'Z' && c != '+' && c != '-') {
                        seconds = parseInt(date, offset, offset + 2)
                        offset += 2
                        if (seconds in 60..62) seconds = 59 // truncate up to 3 leap seconds
                        // milliseconds can be optional in the format
                        if (checkOffset(date, offset, '.')) {
                            offset += 1
                            val endOffset = indexOfNonDigit(date, offset + 1) // assume at least one digit
                            val parseEndOffset = Math.min(endOffset, offset + 3) // parse up to 3 digits
                            val fraction = parseInt(date, offset, parseEndOffset)
                            milliseconds = (Math.pow(10.0, (3 - (parseEndOffset - offset)).toDouble()) * fraction).toInt()
                            offset = endOffset
                        }
                    }
                }
            }

            // extract timezone
            if (date.length <= offset) {
                throw IllegalArgumentException("No time zone indicator")
            }

            val timezone: TimeZone
            val timezoneIndicator = date[offset]

            if (timezoneIndicator == 'Z') {
                timezone = TIMEZONE_Z
            } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                val timezoneOffset = date.substring(offset)
                // 18-Jun-2015, tatu: Minor simplification, skip offset of "+0000"/"+00:00"
                if ("+0000" == timezoneOffset || "+00:00" == timezoneOffset) {
                    timezone = TIMEZONE_Z
                } else {
                    // 18-Jun-2015, tatu: Looks like offsets only work from GMT, not UTC...
                    //    not sure why, but it is what it is.
                    val timezoneId = GMT_ID + timezoneOffset
                    timezone = TimeZone.getTimeZone(timezoneId)
                    val act = timezone.id
                    if (act != timezoneId) {
                        /* 22-Jan-2015, tatu: Looks like canonical version has colons, but we may be given
                         *    one without. If so, don't sweat.
                         *   Yes, very inefficient. Hopefully not hit often.
                         *   If it becomes a perf problem, add 'loose' comparison instead.
                         */
                        val cleaned = act.replace(":", "")
                        if (cleaned != timezoneId) {
                            throw IndexOutOfBoundsException("Mismatching time zone indicator: "
                                    + timezoneId + " given, resolves to " + timezone.id)
                        }
                    }
                }
            } else {
                throw IndexOutOfBoundsException(
                        "Invalid time zone indicator '$timezoneIndicator'")
            }

            val calendar = GregorianCalendar(timezone)
            calendar.isLenient = false
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minutes)
            calendar.set(Calendar.SECOND, seconds)
            calendar.set(Calendar.MILLISECOND, milliseconds)

            return calendar.time
            // If we get a ParseException it'll already have the right message/offset.
            // Other exception types can convert here.
        } catch (e: IndexOutOfBoundsException) {
            throw JsonDataException("Not an RFC 3339 date: " + date)
        } catch (e: IllegalArgumentException) {
            throw JsonDataException("Not an RFC 3339 date: " + date)
        }

    }

    /**
     * Check if the expected character exist at the given offset in the value.

     * @param value the string to check at the specified offset
     * *
     * @param offset the offset to look for the expected character
     * *
     * @param expected the expected character
     * *
     * @return true if the expected character exist at the given offset
     */
    private fun checkOffset(value: String, offset: Int, expected: Char): Boolean {
        return offset < value.length && value[offset] == expected
    }

    /**
     * Parse an integer located between 2 given offsets in a string

     * @param value the string to parse
     * *
     * @param beginIndex the start index for the integer in the string
     * *
     * @param endIndex the end index for the integer in the string
     * *
     * @return the int
     * *
     * @throws NumberFormatException if the value is not a number
     */
    @Throws(NumberFormatException::class)
    private fun parseInt(value: String, beginIndex: Int, endIndex: Int): Int {
        if (beginIndex < 0 || endIndex > value.length || beginIndex > endIndex) {
            throw NumberFormatException(value)
        }
        // use same logic as in Integer.parseInt() but less generic we're not supporting negative values
        var i = beginIndex
        var result = 0
        var digit: Int
        if (i < endIndex) {
            digit = Character.digit(value[i++], 10)
            if (digit < 0) {
                throw NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex))
            }
            result = -digit
        }
        while (i < endIndex) {
            digit = Character.digit(value[i++], 10)
            if (digit < 0) {
                throw NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex))
            }
            result *= 10
            result -= digit
        }
        return -result
    }

    /**
     * Returns the index of the first character in the string that is not a digit, starting at
     * offset.
     */
    private fun indexOfNonDigit(string: String, offset: Int): Int {
        for (i in offset..string.length - 1) {
            val c = string[i]
            if (c < '0' || c > '9') return i
        }
        return string.length
    }
}