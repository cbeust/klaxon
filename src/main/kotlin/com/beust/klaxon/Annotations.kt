package com.beust.klaxon

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class Annotations {
    companion object {
        /**
         * Attempts to find a @Json annotation on the given property by looking through the fields
         * and then the properties of the class.
         */
        fun findJsonAnnotation(kc: KClass<*>, propertyName: String) : Json? {
            try {
                val r1 = kc.java.getDeclaredField(propertyName).annotations.firstOrNull {
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
            } catch(ex: NoSuchFieldException) {
                return null
            }
        }

        fun findProperties(kc: KClass<*>?): Collection<KProperty1<out Any, Any?>> = try {
                if (kc != null) kc.declaredMemberProperties else emptyList()
            } catch (ex: Throwable) {
                // https://youtrack.jetbrains.com/issue/KT-16616
                emptyList()
            }

        fun findNonIgnoredProperties(kc: KClass<*>?): List<KProperty1<out Any, Any?>> {
            val result = findProperties(kc).filter {
                val ignored = it.findAnnotation<Json>()?.ignored
                it.visibility == KVisibility.PUBLIC && (ignored == null || ignored == false)
            }
            return result
        }

        fun findJsonPaths(kc: KClass<*>?): Set<String> {
            val result = hashSetOf<String>()
            val others = arrayListOf<KClass<*>>()
            val thesePaths = findProperties(kc)
                    .mapNotNull {
                        val c = it.returnType.classifier as KClass<*>
                        others.add(c)
                        it.findAnnotation<Json>()
                    }
                    .map { it.path }
            val recursive = others.flatMap{ findJsonPaths(it) }
            result.addAll(thesePaths)
            result.addAll(recursive)
//            println("JSON PATHS FOR $kc: $result")
            return result
        }
    }
}