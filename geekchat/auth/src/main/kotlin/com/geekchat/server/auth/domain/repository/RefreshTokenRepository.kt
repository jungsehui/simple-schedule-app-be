package com.geekchat.server.auth.domain.repository

import com.geekchat.server.auth.domain.model.RefreshToken

interface RefreshTokenRepository {
    fun findByToken(token: String): RefreshToken?
    fun save(refreshToken: RefreshToken): RefreshToken
    fun deleteByToken(token: String)
    fun deleteAllByUserId(userId: String)
}
