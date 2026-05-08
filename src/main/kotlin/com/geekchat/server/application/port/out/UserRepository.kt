package com.geekchat.server.application.port.out

import com.geekchat.server.domain.model.AuthProvider
import com.geekchat.server.domain.model.User
import com.geekchat.server.domain.model.UserProvider

interface UserRepository {
    fun findById(id: String): User?
    fun findActiveById(id: String): User?
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
    fun searchByNickname(query: String, excludeUserId: String, limit: Int = 10): List<User>
    fun searchByUsername(username: String, excludeUserId: String): User?
    fun save(user: User): User
    fun existsByUsername(username: String): Boolean
    fun existsByEmailAndStatusActive(email: String): Boolean
}

interface UserProviderRepository {
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): UserProvider?
    fun findByEmail(email: String): UserProvider?
    fun findAllByUserId(userId: String): List<UserProvider>
    fun save(userProvider: UserProvider): UserProvider
    fun deleteAllByUserId(userId: String)
}
