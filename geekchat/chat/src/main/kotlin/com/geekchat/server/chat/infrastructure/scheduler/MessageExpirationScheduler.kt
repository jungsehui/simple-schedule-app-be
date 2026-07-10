package com.geekchat.server.chat.infrastructure.scheduler
import com.geekchat.server.chat.domain.event.*

import com.geekchat.server.chat.domain.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class MessageExpirationScheduler(
    private val messageRepository: MessageRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 60_000) // every 1 minute
    @Transactional
    fun cleanupExpiredMessages() {
        val now = Instant.now()
        val expired = messageRepository.findExpiredMessages(now)

        if (expired.isEmpty()) return

        val grouped = expired.groupBy { it.chatRoomId }

        for ((roomId, messages) in grouped) {
            val ids = messages.map { it.id }
            messageRepository.hardDeleteByIds(ids)

            eventPublisher.publishEvent(MessageExpired(roomId = roomId, messageIds = ids))
            log.info("messages_expired roomId={} count={}", roomId, ids.size)
        }
    }
}
