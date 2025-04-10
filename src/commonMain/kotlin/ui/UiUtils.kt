@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.MutableStateValue
import utils.FwdInvFunction

inline val Float.dpx get() = Dp.fromPx(this)
inline val Double.dpx get() = Dp.fromPx(this.toFloat())

class MappedStateValue<A : Any?, B : Any?>(private val source: MutableStateValue<B>, pMapper: FwdInvFunction<A, B>) :
    MutableStateValue<A>(pMapper.inverse(source.value)) {

    var function: FwdInvFunction<A, B> = pMapper
        set(value) {
            field = value
            set(value.inverse(source.value))
        }

    init {
        onChange { _, new -> source.set(function(new)) }
        source.onChange { _, new -> set(function.inverse(new)) }
    }
}