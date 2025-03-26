@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.modules.ksl.lang.*

val KslScopeBuilder.NaN get() = Float.NaN.toRawBits().const.toFloatBits()
val KslScopeBuilder.NaN2 get() = Float.NaN.toRawBits().const2.toFloatBits()
val KslScopeBuilder.NaN3 get() = Float.NaN.toRawBits().const3.toFloatBits()
val KslScopeBuilder.NaN4 get() = Float.NaN.toRawBits().const4.toFloatBits()

//region Relation Operators
inline infix fun <S> KslScalarExpression<S>.`==`(right: KslScalarExpression<S>)
    where S : KslType, S : KslScalar = eq(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`==`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = eq(right)


inline infix fun <S> KslScalarExpression<S>.`!=`(right: KslScalarExpression<S>)
    where S : KslType, S : KslScalar = ne(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`!=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = ne(right)


inline infix fun <S> KslScalarExpression<S>.`{`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = lt(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`{`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = lt(right)


inline infix fun <S> KslScalarExpression<S>.`{=`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = le(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`{=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = le(right)


inline infix fun <S> KslScalarExpression<S>.`}`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = gt(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`}`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = gt(right)


inline infix fun <S> KslScalarExpression<S>.`}=`(right: KslScalarExpression<S>)
    where S : KslNumericType, S : KslScalar = ge(right)

inline infix fun <V, S> KslVectorExpression<V, S>.`}=`(right: KslVectorExpression<V, S>)
    where V : KslNumericType, V : KslVector<S>, S : KslScalar = ge(right)
//endregion