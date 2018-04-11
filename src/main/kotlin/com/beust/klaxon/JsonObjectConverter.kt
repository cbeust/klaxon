package com.beust.klaxon

import com.beust.klaxon.internal.firstNotNullResult
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
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
        val result = concreteClass.constructors.firstNotNullResult { constructor ->
            val parameterMap = hashMapOf<KParameter,Any>()
            constructor.parameters.forEach { parameter ->
                map[parameter.name]?.let { convertedValue ->
                    parameterMap[parameter] = convertedValue
                    klaxon.log("Parameter $parameter=$convertedValue (${convertedValue::class})")
                }
            }
            try {
                constructor.isAccessible = true
                constructor.callBy(parameterMap)
            } catch(ex: Exception) {
                // Lazy way to find out of that constructor worked. Easier than trying to make sure each
                // parameter matches the parameter type.
                error = ex::class.qualifiedName + " " + ex.message
                null
            }
        }

        return result ?: throw KlaxonException(
                "Couldn't find a suitable constructor for class ${kc.simpleName} to initialize with $map: $error")
    }

    /**
     * Retrieve all the properties found on the class of the object and then look up each of these
     * properties names on `jsonObject`.
     */
    private fun retrieveKeyValues(jsonObject: JsonObject, kc: KClass<*>) : Map<String, Any> {
        val result = hashMapOf<String, Any>()

        // Only keep the properties that are public and do not have @Json(ignored = true)
        val allProperties = Annotations.findNonIgnoredProperties(kc)

        allProperties.forEach { thisProp ->
            //
            // Check if the name of the field was overridden with a @Json annotation
            //
            val prop = kc.memberProperties.first { it.name == thisProp.name }
            val jsonAnnotation = Annotations.findJsonAnnotation(kc, prop.name)
            val fieldName =
                    if (jsonAnnotation != null && jsonAnnotation.name != "") jsonAnnotation.name
                    else prop.name
            val path = if (jsonAnnotation?.path != "") jsonAnnotation?.path else null

            // Retrieve the value of that property and convert it from JSON
            val jValue = jsonObject[fieldName]

            if (path == null) {
                if (jValue != null) {
                    val convertedValue = klaxon.findConverterFromClass(kc.java, prop)
                            .fromJson(JsonValue(jValue, prop.returnType.javaType,
                                    prop.returnType, klaxon))
                    if (convertedValue != null) {
                        result[prop.name] = convertedValue
                    } else {
                        throw KlaxonException("Don't know how to convert \"$jValue\" into ${prop::class} for "
                                + "field named \"${prop.name}\"")
                    }
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