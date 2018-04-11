package com.beust.klaxon

/**
 * Convert an enum to and from JSON.
 */
class EnumConverter: Converter {
    override fun toJson(value: Any): String {
        return "\"" + value.toString() + "\""
    }

    override fun canConvert(cls: Class<*>): Boolean {
        return cls.isEnum
    }

    override fun fromJson(jv: JsonValue): Enum<*> {
        val javaClass = jv.propertyClass
        val result : Enum<*> =
            if (javaClass is Class<*> && javaClass.isEnum) {
                val valueOf = javaClass.getMethod("valueOf", String::class.java)
                valueOf.invoke(null, jv.inside) as Enum<*>
            } else {
                throw IllegalArgumentException("Could not convert $jv into an enum")
            }
        return result
    }
}
