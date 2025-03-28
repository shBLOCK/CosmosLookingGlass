@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.modules.ui2.Dp

inline val Float.dpx get() = Dp.fromPx(this)
inline val Double.dpx get() = Dp.fromPx(this.toFloat())