package com.geekchat.server.domain.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EitherTest {

    @Test
    fun `Right fold returns onRight result`() {
        val either: Either<String, Int> = Either.Right(42)
        val result = either.fold(onLeft = { "left" }, onRight = { "right:$it" })
        assertEquals("right:42", result)
    }

    @Test
    fun `Left fold returns onLeft result`() {
        val either: Either<String, Int> = Either.Left("error")
        val result = either.fold(onLeft = { "left:$it" }, onRight = { "right" })
        assertEquals("left:error", result)
    }

    @Test
    fun `map transforms Right value`() {
        val either: Either<String, Int> = Either.Right(10)
        val mapped = either.map { it * 2 }
        assertEquals(Either.Right(20), mapped)
    }

    @Test
    fun `map does not transform Left`() {
        val either: Either<String, Int> = Either.Left("err")
        val mapped = either.map { it * 2 }
        assertEquals(Either.Left("err"), mapped)
    }

    @Test
    fun `flatMap chains Right values`() {
        val either: Either<String, Int> = Either.Right(5)
        val result = either.flatMap { Either.Right(it + 10) }
        assertEquals(Either.Right(15), result)
    }

    @Test
    fun `flatMap short-circuits on Left`() {
        val either: Either<String, Int> = Either.Left("fail")
        val result = either.flatMap { Either.Right(it + 10) }
        assertEquals(Either.Left("fail"), result)
    }

    @Test
    fun `getOrNull returns value for Right`() {
        assertEquals(42, Either.Right(42).getOrNull())
    }

    @Test
    fun `getOrNull returns null for Left`() {
        assertNull(Either.Left("err").getOrNull())
    }

    @Test
    fun `isLeft and isRight flags`() {
        assertTrue(Either.Left("err").isLeft)
        assertFalse(Either.Left("err").isRight)
        assertTrue(Either.Right(1).isRight)
        assertFalse(Either.Right(1).isLeft)
    }

    @Test
    fun `getOrElse returns value for Right`() {
        val result = Either.Right(42).getOrElse { -1 }
        assertEquals(42, result)
    }

    @Test
    fun `getOrElse returns default for Left`() {
        val result = Either.Left("err").getOrElse { -1 }
        assertEquals(-1, result)
    }
}
