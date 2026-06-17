package com.geekchat.server.common.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class SoftDeletableJpaEntity(
    id: String = "",
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
) : BaseJpaEntity(id, createdAt, updatedAt) {

    fun softDelete() {
        deletedAt = Instant.now()
        updatedAt = Instant.now()
    }

    val isDeleted: Boolean
        get() = deletedAt != null
}
