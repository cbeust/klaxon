package com.beust.klaxon

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class Reflection {
    companion object {
        fun isAssignableFromAny(type: Class<*>, vararg kc: KClass<*>)
                = kc.any { type.isAssignableFrom(type) }
    }
}

enum class Visibility { PUBLIC, PRIVATE }

class Property1(val name: String,
        val getter: Method, val javaSetter: Method? = null, val javaField: Field? = null) {
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
    val typeFor: TypeFor? =
            getter.getAnnotation(TypeFor::class.java)
    val json: Json? =
            getter.getAnnotation(Json::class.java)
//    fun <T> findAnnotation(): KClass<*>? {
//        return null
//    }
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
                        Property1(name, it, setter)
                    }
            result.addAll(properties)
            cl = cl.superclass
        }
        return result
    }

val KClass<*>.fixedDeclaredMemberProperties: List<Property1>
    get() = TODO()
