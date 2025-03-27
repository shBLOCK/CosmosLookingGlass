@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.modules.ksl.lang.*

val KslScopeBuilder.NaN get() = Float.NaN.toRawBits().const.toFloatBits()
val KslScopeBuilder.NaN2 get() = Float.NaN.toRawBits().const2.toFloatBits()
val KslScopeBuilder.NaN3 get() = Float.NaN.toRawBits().const3.toFloatBits()
val KslScopeBuilder.NaN4 get() = Float.NaN.toRawBits().const4.toFloatBits()

//region Relation Operators
infix fun <S> KslScalarExpression<S>.`==`(right: KslScalarExpression<S>)
    where S : KslType, S : KslScalar = eq(right)

infix fun <V, S> KslVectorExpression<V, S>.`==`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = eq(right)


infix fun <S> KslScalarExpression<S>.`!=`(right: KslScalarExpression<S>)
    where S : KslType, S : KslScalar = ne(right)

infix fun <V, S> KslVectorExpression<V, S>.`!=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = ne(right)


infix fun <S> KslScalarExpression<S>.`{`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = lt(right)

infix fun <V, S> KslVectorExpression<V, S>.`{`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = lt(right)


infix fun <S> KslScalarExpression<S>.`{=`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = le(right)

infix fun <V, S> KslVectorExpression<V, S>.`{=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = le(right)


infix fun <S> KslScalarExpression<S>.`}`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = gt(right)

infix fun <V, S> KslVectorExpression<V, S>.`}`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = gt(right)


infix fun <S> KslScalarExpression<S>.`}=`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = ge(right)

infix fun <V, S> KslVectorExpression<V, S>.`}=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = ge(right)
//endregion

//TODO: remove when mod if officially supported
fun KslScopeBuilder.mod(x: KslScalarExpression<KslFloat1>, y: KslScalarExpression<KslFloat1>) = x - y * floor(x / y)