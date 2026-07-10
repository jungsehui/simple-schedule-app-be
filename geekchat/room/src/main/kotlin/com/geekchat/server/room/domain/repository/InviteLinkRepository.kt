package com.geekchat.server.room.domain.repository

import com.geekchat.server.room.domain.model.InviteLink

interface InviteLinkRepository {
    fun findByCode(code: String): InviteLink?
    fun save(inviteLink: InviteLink): InviteLink
}
