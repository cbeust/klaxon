package com.beust.klaxon

/**
 * Convert an enum to and from JSON.
 */
class EnumConverter: Converter {
    override fun toJson(value: Any): String {
        val enum = value as Enum<*>
        val enumClass = if (value.javaClass.isEnum) {
            value.javaClass
        } else if (value.javaClass.superclass.isEnum) {
            value.javaClass.superclass
        } else {
            throw IllegalArgumentException("Could not find associated enum class for $value")
        }
        val field = enumClass.declaredFields.find { it.name == enum.name }
            ?: throw IllegalArgumentException("Could not find associated enum field for $value")
        return "\"${field.getAnnotation(Json::class.java)?.name ?: enum.name}\""
    }

    override fun canConvert(cls: Class<*>): Boolean {
        return cls.isEnum
    }

    override fun fromJson(jv: JsonValue): Enum<*> {
        val javaClass = jv.propertyClass
        if (javaClass !is Class<*> || !javaClass.isEnum) {
            throw IllegalArgumentException("Could not convert $jv into an enum")
        }
        val name = jv.inside as String
        val field = javaClass.declaredFields
            .find { it.name == name || it.getAnnotation(Json::class.java)?.name == name }
            ?: throw IllegalArgumentException("Could not find enum value for $name");
        return field.get(null) as Enum<*>
    }
}
