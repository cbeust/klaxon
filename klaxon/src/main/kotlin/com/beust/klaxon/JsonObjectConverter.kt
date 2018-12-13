package com.beust.klaxon

import com.beust.klaxon.internal.firstNotNullResult
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
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
        val result =
            if (kc == Map::class) {
                initIntoMap(jsonObject, kc)
            } else {
                initIntoUserClass(jsonObject, kc)
            }
        return result
    }

    private fun initIntoMap(jsonObject: JsonObject, kc: KClass<*>): Any {
        val result = hashMapOf<String, Any?>()
        jsonObject.keys.forEach { key ->
            result[key] = jsonObject[key]
        }
        return result
    }

    private fun initIntoUserClass(jsonObject: JsonObject, kc: KClass<*>): Any {
        val concreteClass = if (Annotations.isList(kc)) ArrayList::class else kc

        // Go through all the Kotlin constructors and associate each parameter with its value.
        // (Kotlin constructors contain the names of their parameters).
        // Note that this code will work for default parameters as well: values missing in the JSON map
        // will be filled by Kotlin reflection if they can't be found.
        val map = retrieveKeyValues(jsonObject, kc)
        val errorMessage = arrayListOf<String>()
        val result = concreteClass.constructors.firstNotNullResult { constructor ->
            val parameterMap = hashMapOf<KParameter, Any?>()
            constructor.parameters.forEach { parameter ->
                if (map.containsKey(parameter.name)) {
                    val convertedValue = map[parameter.name]
//                    parameterMap[parameter] = adjustType(convertedValue, parameter)
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
                null
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
                "Couldn't find a suitable constructor for class ${kc.simpleName} to initialize with $map")
    }

    private fun adjustType(convertedValue: Any?, parameter: KParameter): Any? {
        var result = convertedValue
        val cj = convertedValue!!::class.java
        if (cj.isArray) {
            val cl = parameter.type.classifier
            if (cl is KClass<*>) {
                if (IntArray::class.java.isAssignableFrom(cl.java)) {
                    val array = convertedValue as IntArray
                    val ac = array::class
                    val componentType = (ac as Class<*>).componentType
                    val realArray = java.lang.reflect.Array.newInstance(componentType, array.size)
                    result = realArray//IntArray(array.size)
                    array.forEachIndexed{ i, v ->
//                        result.set(i, v)
                        java.lang.reflect.Array.set(realArray, i, v)
                    }
                    println("Array: $array")
                }
            }
        }
        return result
    }

    /**
     * Retrieve all the properties found on the class of the object and then look up each of these
     * properties names on `jsonObject`, which came from the JSON document.
     */
    private fun retrieveKeyValues(jsonObject: JsonObject, kc: KClass<*>) : Map<String, Any?> {
        val result = hashMapOf<String, Any?>()

        // Only keep the properties that are public and do not have @Json(ignored = true)
        val allProperties = Annotations.findNonIgnoredProperties(kc, klaxon.propertyStrategies)

        // See if have any polymorphic properties
        val polymorphicMap = findPolymorphicProperties(allProperties)

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
                    // Look up the polymorphic info for that field. If there is one, we need to
                    // retrieve its TypeAdapter
                    val polymorphicInfo = polymorphicMap[fieldName]

                    val polymorphicClass =
                        if (polymorphicInfo != null) {
                            // We have polymorphic information for this field. Retrieve its TypeAdapter,
                            // instantiate it, and invoke it with the discriminant value.
                            val discriminant = jsonObject[polymorphicInfo.typeFieldName] as Any
                            polymorphicInfo.adapter.createInstance().instantiate(discriminant)
                        } else {
                            null
                        }

                    val kClass = polymorphicClass ?: kc
                    val ktype = if (polymorphicClass != null) kClass.createType() else prop.returnType
                    val cls = polymorphicClass?.java ?: kc.java
                    val convertedValue = klaxon.findConverterFromClass(cls, prop)
                            .fromJson(JsonValue(jValue, prop.returnType.javaType,
                                    ktype, klaxon))
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

    class PolymorphicInfo(val typeFieldName: String, val valueFieldName: String,
            val adapter: KClass<out TypeAdapter<*>>)

    private fun findPolymorphicProperties(allProperties: List<KProperty1<out Any, Any?>>)
            : Map<String, PolymorphicInfo> {
        val result = hashMapOf<String, PolymorphicInfo>()
        allProperties.forEach {
            it.findAnnotation<TypeFor>()?.let { typeForAnnotation ->
                typeForAnnotation.field.let { field ->
                    result[field] = PolymorphicInfo(it.name, field, typeForAnnotation.adapter)
                }
            }
        }
        return result
    }
}