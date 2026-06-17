package com.geekchat.server.application.service

import com.geekchat.server.application.port.out.UserRepository
import com.geekchat.server.common.error.ChatError
import com.geekchat.server.common.error.Either
import com.geekchat.server.domain.model.User
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun searchUsers(query: String, currentUserId: String): Either<ChatError, List<User>> {
        if (query.isBlank()) {
            return Either.Right(emptyList())
        }

        val results = if (query.startsWith("@")) {
            val username = query.removePrefix("@")
            val user = userRepository.searchByUsername(username, currentUserId)
            if (user != null) listOf(user) else emptyList()
        } else {
            userRepository.searchByNickname(query, currentUserId, limit = 10)
        }

        return Either.Right(results)
    }

    fun setUsername(userId: String, username: String): Either<ChatError, User> {
        val user = userRepository.findById(userId)
            ?: return Either.Left(ChatError.UserNotFound(userId))

        if (user.username == username) {
            return Either.Right(user)
        }

        if (userRepository.existsByUsername(username)) {
            return Either.Left(ChatError.UsernameAlreadyTaken(username))
        }

        val updated = try {
            user.withUsername(username)
        } catch (_: IllegalArgumentException) {
            return Either.Left(ChatError.InvalidUsername())
        }

        return Either.Right(userRepository.save(updated))
    }
}
