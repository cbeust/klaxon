package com.beust.klaxon

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptySet
import kotlin.Comparator
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

        private fun findProperties(kc: KClass<*>?): Collection<KProperty1<out Any, Any?>> = try {
            (kc?.memberProperties ?: emptyList()).sortedWith(Comparator() { o1, o2 ->
                val j1 = o1.findAnnotation<Json>()
                val j2 = o2.findAnnotation<Json>()
                if (j1 == null || j2 == null) 0
                    else j1.index.compareTo(j2.index)
            })
        } catch (ex: Throwable) {
            // https://youtrack.jetbrains.com/issue/KT-16616
            emptyList()
        }

		fun findNonIgnoredProperties(kc: KClass<*>?, strategies: List<PropertyStrategy>): List<KProperty1<out Any, Any?>> =
				findProperties(kc).filter {
					// Visibility
					val ignored = it.findAnnotation<Json>()?.ignored
					it.visibility == KVisibility.PUBLIC && (ignored == null || ignored == false) ||
							it.visibility == KVisibility.PRIVATE && (ignored != null || ignored == false)
				}.filter { property ->
					// PropertyStrategy
					strategies.none { !it.accept(property) }
				}

        fun findJsonPaths(kc: KClass<*>?) = findJsonPaths(kc, HashSet())

        private fun findJsonPaths(kc: KClass<*>?, seen: HashSet<KClass<*>>): Set<String> {
            val result = hashSetOf<String>()
            val others = arrayListOf<KClass<*>>()
            val thesePaths = findProperties(kc)
                    .mapNotNull {
                        val classifier = it.returnType.classifier
                        if (classifier is KClass<*>) others.add(classifier)
                        else {
                            // Generic type, ignore
                        }
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

        fun retrieveJsonFieldName(klaxon: Klaxon, kc: KClass<*>, prop: KProperty1<*, *>) : String {
            val jsonAnnotation = Annotations.findJsonAnnotation(kc, prop.name)
            val fieldName =
                    if (jsonAnnotation != null && jsonAnnotation.nameInitialized()) jsonAnnotation.name
                    else prop.name
            val result = klaxon.fieldRenamer?.toJson(fieldName) ?: fieldName
            return result
        }
    }


}