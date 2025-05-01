@file:Suppress("NOTHING_TO_INLINE")

package ui

import utils.State
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class KeyedStateStore {
    @PublishedApi
    internal var root = Node(parent = null)

    @PublishedApi
    internal var currentNode = root

    @PublishedApi
    internal var currentTouchId = Int.MIN_VALUE

    fun beginCycle() {
        check(currentNode === root)
        currentTouchId++
        root.rewind()
    }

    fun endCycle() {
        check(currentNode === root)
        root.prune()
    }

    inline fun withKey(key: Any?, block: () -> Any) {
        currentNode = currentNode.child(key)
        block()
        currentNode = currentNode.parent!!
    }

    @PublishedApi
    internal inner class Node(val parent: Node?) {
        var isFirstUse = true
        var pos = 0
        val values = mutableListOf<Any?>()
        val children = mutableMapOf<Any?, Node>()
        var touchId = Int.MIN_VALUE

        inline fun child(key: Any?) =
            children.getOrPut(key) { Node(parent = this) }.also { it.touchId = currentTouchId }

        inline fun <reified T : Any> value(init: () -> T): T {
            if (isFirstUse) return init().also { values.add(it) }
            return recallValue(T::class)
        }

        @PublishedApi
        internal fun <T : Any> recallValue(type: KClass<T>): T {
            val value = values[pos++]
            return type.safeCast(value)
                ?: throw IllegalStateException("The remembered value <$value> at index ${pos - 1} doesn't have the expected type <${type.qualifiedName}>.")
        }

        fun rewind() {
            isFirstUse = false
            pos = 0
            children.forEach { (_, child) -> child.rewind() }
        }

        fun prune() {
            val iter = children.iterator()
            while (iter.hasNext()) {
                val (_, child) = iter.next()
                if (child.touchId != currentTouchId) {
                    iter.remove()
                    continue
                }
                child.prune()
            }
        }
    }

    interface Managed {
        val stateStore: KeyedStateStore

        fun <R : Any?> KeyedStateStore.cycle(block: () -> R): R {
            check(this === stateStore)
            beginCycle()
            return block().also { endCycle() }
        }
    }
}

inline fun KeyedStateStore.Managed.withKey(key: Any?, block: () -> Any) = stateStore.withKey(key, block)
inline fun <reified T : Any> KeyedStateStore.Managed.keyed(init: () -> T) = stateStore.currentNode.value(init)
inline fun <T : Any?> KeyedStateStore.Managed.keyedState(init: () -> T) = keyed { State(init()) }