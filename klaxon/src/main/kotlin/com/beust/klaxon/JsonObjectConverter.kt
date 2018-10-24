package com.beust.klaxon

import com.beust.klaxon.internal.firstNotNullResult
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.javaType

/**
 * Convert a JsonObject into a class instance.
 */
class JsonObjectConverter(private val klaxon: Klaxon, private val allPaths: HashMap<String, Any>) {
    /**
     * Go through all the constructors found on that object and attempt to invoke them with the key values
     * found on the object. We return the first successful instantiation, or fail with an exception if
     * no suitable constructor was found.
     */
    fun fromJson(jsonObject: JsonObject, kc: KClass<*>): Any {
        val concreteClass = if (Annotations.isList(kc)) ArrayList::class else kc

        // Go through all the Kotlin constructors and associate each parameter with its value.
        // (Kotlin constructors contain the names of their parameters).
        // Note that this code will work for default parameters as well: values missing in the JSON map
        // will be filled by Kotlin reflection if they can't be found.
        var error: String? = null
        val map = retrieveKeyValues(jsonObject, kc)
        var errorMessage = arrayListOf<String>()
        val result = concreteClass.constructors.firstNotNullResult { constructor ->
            val parameterMap = hashMapOf<KParameter, Any?>()
            constructor.parameters.forEach { parameter ->
                if (map.containsKey(parameter.name)) {
                    val convertedValue = map[parameter.name]
                    parameterMap[parameter] = convertedValue
                    val valueClass = if (convertedValue != null) convertedValue::class else "null"
                    klaxon.log("Parameter $parameter=$convertedValue ($valueClass)")
                }
            }
            try {
                constructor.isAccessible = true
                constructor.callBy(parameterMap)
            } catch(ex: Exception) {
                // Lazy way to find out of that constructor worked. Easier than trying to make sure each
                // parameter matches the parameter type.
                errorMessage.add("Unable to instantiate ${concreteClass.simpleName}" +
                        " with parameters " +
                        parameterMap.entries.map { it.key.name.toString() + ": " + it.value.toString() })
//                throw KlaxonException(errorMessage)
            }
        }

        if (errorMessage.any()) {
            throw KlaxonException(errorMessage.joinToString("\n"))
        }

        // Now that we have an initialized object, find all the other non constructor properties
        // and if we have a value from JSON for them, initialize them as well. @@@
        val properties = Annotations.findNonIgnoredProperties(kc, klaxon.propertyStrategies)
        properties.filter {
            it.name in map
        }.forEach {
            if (it is KMutableProperty<*>) {
                val value = map[it.name]
                if (value != null) {
                    it.javaSetter!!.invoke(result, value)
                }
            } else {
                // Mutable property
                val field = it.javaField
                if (field != null && result != null) {
                    val value = map[it.name]
                    field.isAccessible = true
                    field.set(result, value)
                } else {
                    klaxon.log("Ignoring read-only property $it")
                }
            }
        }

        return result ?: throw KlaxonException(
                "Couldn't find a suitable constructor for class ${kc.simpleName} to initialize with $map: $error")
    }

    /**
     * Retrieve all the properties found on the class of the object and then look up each of these
     * properties names on `jsonObject`, which came from the JSON document.
     */
    private fun retrieveKeyValues(jsonObject: JsonObject, kc: KClass<*>) : Map<String, Any?> {
        val result = hashMapOf<String, Any?>()

        // Only keep the properties that are public and do not have @Json(ignored = true)
        val allProperties = Annotations.findNonIgnoredProperties(kc, klaxon.propertyStrategies)

        allProperties.forEach { thisProp ->
            //
            // Check if the name of the field was overridden with a @Json annotation
            //
            val prop = kc.memberProperties.first { it.name == thisProp.name }
            val fieldName = Annotations.retrieveJsonFieldName(klaxon, kc, prop)
            val jsonAnnotation = Annotations.findJsonAnnotation(kc, prop.name)
            val path = if (jsonAnnotation?.path != "") jsonAnnotation?.path else null

            // Retrieve the value of that property and convert it from JSON
            val jValue = jsonObject[fieldName]

            if (path == null) {
                // Note: use containsKey here since it's valid for a JSON object to have a value spelled "null"
                if (jsonObject.containsKey(fieldName)) {
                    val convertedValue = klaxon.findConverterFromClass(kc.java, prop)
                            .fromJson(JsonValue(jValue, prop.returnType.javaType,
                                    prop.returnType, klaxon))
                    result[prop.name] = convertedValue
                } else {
                    // Didn't find any value for that property: don't do anything. If a value is missing here,
                    // it might still be found as a default value on the constructor, and we'll find out once we
                    // try to instantiate that object.
                }
            } else {
                result[prop.name] = allPaths[path]
                        ?: throw KlaxonException("Couldn't find path \"$path\" specified on field \"${prop.name}\"")

            }
        }
        return result
    }
}