package com.geekchat.server.domain.error

sealed class Either<out L, out R> {
    data class Left<L>(val value: L) : Either<L, Nothing>()
    data class Right<R>(val value: R) : Either<Nothing, R>()

    fun <T> fold(onLeft: (L) -> T, onRight: (R) -> T): T = when (this) {
        is Left -> onLeft(value)
        is Right -> onRight(value)
    }

    fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this
        is Right -> Right(transform(value))
    }

    fun <T> flatMap(transform: (R) -> Either<@UnsafeVariance L, T>): Either<L, T> = when (this) {
        is Left -> this
        is Right -> transform(value)
    }

    val isLeft: Boolean get() = this is Left
    val isRight: Boolean get() = this is Right

    fun getOrNull(): R? = when (this) {
        is Left -> null
        is Right -> value
    }
}

fun <L, R> Either<L, R>.getOrElse(default: (L) -> R): R = when (this) {
    is Either.Left -> default(value)
    is Either.Right -> value
}
