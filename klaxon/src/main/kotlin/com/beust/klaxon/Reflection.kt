package com.beust.klaxon

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class Reflection {
    companion object {
        fun isAssignableFromAny(type: Class<*>, vararg kc: KClass<*>)
                = kc.any { type.isAssignableFrom(type) }
    }
}

enum class Visibility { PUBLIC, PRIVATE }

class Property1(val name: String,
        val getter: Method, val javaSetter: Method? = null, val javaField: Field? = null,
        val json: Json? = null,
        val typeFor: TypeFor? = null) {
    fun call(obj: Any): Any? {
        TODO()
    }

    override fun toString() = "{Property1 $name: ${getter.returnType}}"

    val isMutable: Boolean = javaSetter != null
    val returnType: KClass<*>
        get() {
            println("RETURN TYPE FOR $this")
            return getter.returnType.kotlin
//            return kc.createType(kc.typeParameters.map { it.starProjectedType })
        }
    val visibility: Visibility =
            if (Modifier.isPublic(getter.modifiers)) Visibility.PUBLIC
            else Visibility.PRIVATE
}

val KClass<*>.fixedMemberProperties: List<Property1>
    get() {
        val result = arrayListOf<Property1>()
        var cl: Class<*> = this.java
        while (cl != Object::class.java) {
            val properties = cl.methods
                    .filter { it.name != "getClass" }
//                    .filter { it.isAccessible }
                    .filter { it.name.startsWith("get") || it.name.startsWith("is") }
                    .filter { it.parameterCount == 0 }
                    .map {
                        val isBoolean = it.name.startsWith("is")
                        val count = if (isBoolean) 2 else 3
                        val name = if (isBoolean) it.name
                            else it.name[count].toLowerCase() + it.name.substring(count + 1)
                        val setter = try {
                            cl.getMethod("set" + name.capitalize())
                        } catch(ex: NoSuchMethodException) {
                            null
                        }
                        val annotationsMethod = cl.methods.firstOrNull { it.name == name + "\$annotations" }
                        val anns = annotationsMethod?.invoke(null)
                        val p = this.declaredMemberProperties.firstOrNull { it.name == name }
                        val typeFor = p?.findAnnotation<TypeFor>()
                        val json = p?.findAnnotation<Json>()
                        Property1(name, it, setter, null, json, typeFor)
                    }
            result.addAll(properties)
            cl = cl.superclass
        }
        return result
    }

val KClass<*>.fixedDeclaredMemberProperties: List<Property1>
    get() = TODO()
