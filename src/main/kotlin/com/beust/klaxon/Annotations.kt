package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class Annotations {
    companion object {
        /**
         * Attempts to find a @Json annotation on the given property by looking through the fields
         * and then the properties of the class.
         */
        fun findJsonAnnotation(kc: KClass<*>, propertyName: String): Json? {
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
            } catch (ex: NoSuchFieldException) {
                return null
            }
        }

        private fun findProperties(kc: KClass<*>?): Collection<KProperty1<out Any, Any?>> = try {
            if (kc != null) kc.memberProperties else emptyList()
        } catch (ex: Throwable) {
            // https://youtrack.jetbrains.com/issue/KT-16616
            emptyList()
        }

        fun findNonIgnoredProperties(kc: KClass<*>?, strategies: List<PropertyStrategy>)
                : List<KProperty1<out Any, Any?>> {
            val result = findProperties(kc)
                .filter {
                    // Visibility
                    val ignored = it.findAnnotation<Json>()?.ignored
                    it.visibility == KVisibility.PUBLIC && (ignored == null || ignored == false) ||
                            it.visibility == KVisibility.PRIVATE && (ignored != null || ignored == false)
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
                        val c = it.returnType.classifier as KClass<*>
                        others.add(c)
                        it.findAnnotation<Json>()
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

        fun isArray(type: KType?) = type?.jvmErasure is KClass<*> && type.jvmErasure.java.isArray

        fun isSet(type: Type?) =
            if (type is ParameterizedType) {
                Set::class.java.isAssignableFrom(type.rawType as Class<*>)
            } else {
                false
            }

        fun isList(kClass: KClass<*>) = kotlin.collections.List::class.java.isAssignableFrom(kClass.java)
    }


}