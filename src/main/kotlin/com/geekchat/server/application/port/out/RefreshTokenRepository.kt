package com.geekchat.server.application.port.out

import com.geekchat.server.domain.model.RefreshToken

interface RefreshTokenRepository {
    fun findByToken(token: String): RefreshToken?
    fun save(refreshToken: RefreshToken): RefreshToken
    fun deleteByToken(token: String)
    fun deleteAllByUserId(userId: String)
}
