@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.modules.ksl.lang.*
import kotlin.jvm.JvmName

inline val KslScopeBuilder.NaN get() = Float.NaN.toRawBits().const.toFloatBits()
inline val KslScopeBuilder.NaN2 get() = Float.NaN.toRawBits().const2.toFloatBits()
inline val KslScopeBuilder.NaN3 get() = Float.NaN.toRawBits().const3.toFloatBits()
inline val KslScopeBuilder.NaN4 get() = Float.NaN.toRawBits().const4.toFloatBits()

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

//TODO: remove when mod if officially supported
@JvmName("fmod")
inline fun KslScopeBuilder.mod(x: KslScalarExpression<KslFloat1>, y: KslScalarExpression<KslFloat1>) =
    x - y * floor(x / y)

@JvmName("imod")
inline fun KslScopeBuilder.mod(x: KslScalarExpression<KslInt1>, y: KslScalarExpression<KslInt1>) =
    ((x % y) + y) % y

inline fun KslScopeBuilder.cross2(a: KslExprFloat2, b: KslExprFloat2) = determinant(mat2Value(a, b))

inline fun KslScopeBuilder.rotate90(v: KslExprFloat2) = float2Value(-v.y, v.x)

fun KslShaderStage.functionQuatSlerp(name: String = "quatSlerp") = functionFloat4(name) {
    val a = paramFloat4("a")
    val b = paramFloat4("b")
    val t = paramFloat1("t")
    body {
        val b = float4Var(b, "_b")

        val d = float1Var(dot(a, b))
        `if`(d `{` 0F.const) {
            b set -b
            d set -d
        }

        `if`(d `}` 0.9995F.const) {
            `return`(normalize(mix(a, b, t)))
        }

        val theta = float1Var(acos(d) * t)
        val q = float4Var(normalize(b - a * d))
        normalize(a * cos(theta) + q * sin(theta))
    }
}