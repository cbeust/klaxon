package com.beust.klaxon

import java.lang.reflect.Type
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptySet
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class Annotations {
    companion object {
        /**
         * Attempts to find a @Json annotation on the given property by looking through the fields
         * and then the properties of the class.
         */
        fun findJsonAnnotation(kc: KClass<*>, propertyName: String): Json? {
            val result1 = kc.memberProperties.firstOrNull { it.name == propertyName }?.findAnnotation<Json>()
            if (result1 != null) return result1

            try {
                val r1 = kc.java.getField(propertyName).annotations.firstOrNull {
                    it.javaClass == Json::class.java
                }
                val result =
                        if (r1 == null) {
                            val r2 = kc.declaredMemberProperties
                                    .filter { it.name == propertyName }
                                    .mapNotNull { it.findAnnotation<Json>() }
                            if (r2.isEmpty()) null else r2[0]
                        } else {
                            r1
                        }
                return result as Json?
            } catch (ex: NoSuchFieldException) {
                return null
            }
        }

        private fun findProperties(kc: KClass<*>?): Collection<Property1> = try {
            if (kc != null) kc.fixedMemberProperties else emptyList()
        } catch (ex: Throwable) {
            // https://youtrack.jetbrains.com/issue/KT-16616
            emptyList()
        }

        fun findNonIgnoredProperties(kc: KClass<*>?, strategies: List<PropertyStrategy>)
                : List<Property1> {
            val result = findProperties(kc)
                .filter {
                    // Visibility
                    val ignored = it.json?.ignored
                    it.visibility == Visibility.PUBLIC && (ignored == null || ignored == false) ||
                            it.visibility == Visibility.PRIVATE && (ignored != null || ignored == false)
                }.filter {
                    // PropertyStrategy
                    val r = strategies.fold(true) { initial: Boolean, op: PropertyStrategy ->
                        initial and op.accept(it) }
                    val result = strategies.isEmpty() || r
                    result
                }
            return result
        }

        fun findJsonPaths(kc: KClass<*>?) = findJsonPaths(kc, HashSet())

        private fun findJsonPaths(kc: KClass<*>?, seen: HashSet<KClass<*>>): Set<String> {
            val result = hashSetOf<String>()
            val others = arrayListOf<KClass<*>>()
            val thesePaths = findProperties(kc)
                    .mapNotNull {
                        others.add(it.returnType)
//                        val classifier = it.returnType.classifier
//                        if (classifier is KClass<*>) others.add(it.returnType)
//                        else {
//                            // Generic type, ignore
//                        }
                        it.json
                    }
                    .map { it.path }
            val recursive = others.flatMap {
                if (! seen.contains(it)) {
                    seen.add(it)
                    findJsonPaths(it, seen)
                } else {
                    emptySet()
                }
            }
            result.addAll(thesePaths)
            result.addAll(recursive)
//            println("JSON PATHS FOR $kc: $result")
            return result
        }

        fun isArray(type: Type?) = (type is Class<*> && type.isArray) || type is Array<*>
        fun isSet(type: Type?) = Set::class.java.isAssignableFrom(type as Class<*>)
        fun isList(kClass: KClass<*>) = List::class.java.isAssignableFrom(kClass.java)

        fun retrieveJsonFieldName(klaxon: Klaxon, kc: KClass<*>, prop: Property1) : String {
            val jsonAnnotation = findJsonAnnotation(kc, prop.name)
            val fieldName =
                    if (jsonAnnotation != null && jsonAnnotation.nameInitialized()) jsonAnnotation.name
                    else prop.name
            val result = klaxon.fieldRenamer?.toJson(fieldName) ?: fieldName
            return result
        }
    }


}