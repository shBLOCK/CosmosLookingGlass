package utils

interface MutableCollectionMixin<E> : MutableCollection<E> {
    override fun isEmpty(): Boolean = size == 0
    override fun containsAll(elements: Collection<E>) = elements.all(::contains)
    override fun addAll(elements: Collection<E>) = elements.greedyAny(::add)
    override fun removeAll(elements: Collection<E>) = elements.greedyAny(::remove)
    override fun retainAll(elements: Collection<E>) = retainAll(elements::contains)
    override fun clear() = iterator().removeAll()
}