package com.geekchat.server.room.domain.event

/**
 * Published when a user joins a room (direct/group creation or invite-link join).
 *
 * The websocket module subscribes to register the user's live sessions with the
 * room so subsequent broadcasts reach them. Replaces the previous direct call into
 * WebSocketBroadcaster.joinRoom, removing the room→websocket coupling.
 *
 * Handled synchronously (plain @EventListener at the publish point), so timing is
 * identical to the former direct call — this is in-memory session bookkeeping with
 * no transaction-commit ordering constraint.
 */
data class RoomMemberJoined(
    val userId: String,
    val roomId: String,
)
