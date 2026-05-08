package com.geekchat.server.application.port.out

import com.geekchat.server.domain.model.InviteLink

interface InviteLinkRepository {
    fun findByCode(code: String): InviteLink?
    fun save(inviteLink: InviteLink): InviteLink
}
