package com.geekchat.server.room.infrastructure.scheduler
import com.geekchat.server.room.domain.event.*

import com.geekchat.server.room.domain.repository.ChatRoomMemberRepository
import com.geekchat.server.room.domain.repository.ChatRoomRepository
import com.geekchat.server.chat.domain.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component
class RoomExpirationScheduler(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val messageRepository: MessageRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val warnedRoomIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Scheduled(fixedRate = 600_000) // every 10 minutes
    @Transactional
    fun processExpiringRooms() {
        val now = Instant.now()

        // 1. Warn rooms expiring within 10 minutes
        val threshold = now.plus(10, ChronoUnit.MINUTES)
        val expiringRooms = chatRoomRepository.findExpiringRoomsSoon(now, threshold)
        for (room in expiringRooms) {
            if (warnedRoomIds.add(room.id)) {
                eventPublisher.publishEvent(
                    RoomExpiring(roomId = room.id, roomName = room.name, expiresAt = room.expiresAt!!),
                )
                log.info("room_expiring_warning roomId={} expiresAt={}", room.id, room.expiresAt)
            }
        }

        // 2. Cleanup expired rooms
        val expiredRooms = chatRoomRepository.findExpiredRooms(now)
        for (room in expiredRooms) {
            messageRepository.softDeleteByRoomId(room.id, now)
            chatRoomMemberRepository.deleteAllByChatRoomId(room.id)
            chatRoomRepository.softDelete(room.id, now)

            warnedRoomIds.remove(room.id)
            eventPublisher.publishEvent(RoomExpired(roomId = room.id, roomName = room.name))
            log.info("room_expired roomId={} roomName={}", room.id, room.name)
        }

        // 3. Clean stale entries from warnedRoomIds (older than 1 hour)
        if (warnedRoomIds.size > 1000) {
            warnedRoomIds.clear()
        }
    }
}
