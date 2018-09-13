package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.reflect.KProperty

@Test
class Issue180 {
    fun f() {
        val x = Klaxon().toJsonString(CharacterPlayer(12))
        val char = Klaxon().parse<CharacterPlayer>(x)
        assertThat(char!!.id).isEqualTo(12)
    }
}


class CharacterPlayer(val id: Int){

    var characters = listOf<CharacterPlayer>()

    @Json(ignored = true)
    val visibleDelegate = InvalidatableLazyImpl({
        characters.toMutableList()
    })
    @Json(ignored = true)
    val visibleChars: List<CharacterPlayer> by visibleDelegate

}

private object UNINITIALIZED_VALUE
class InvalidatableLazyImpl<T>(private val initializer: () -> T, lock: Any? = null) : Lazy<T> {
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = lock ?: this
    fun invalidate(){
        _value = UNINITIALIZED_VALUE
    }

    override val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    _v2 as T
                }
                else {
                    val typedValue = initializer()
                    _value = typedValue
                    typedValue
                }
            }
        }


    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    operator fun setValue(any: Any, property: KProperty<*>, t: T) {
        _value = t
    }
}