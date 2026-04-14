package com.geekchat.server.domain.error

sealed class ChatError {
    abstract val message: String

    // Auth
    data class TokenNotProvided(override val message: String = "Token not provided") : ChatError()
    data class TokenExpired(override val message: String = "Token expired") : ChatError()
    data class InvalidToken(override val message: String = "Invalid token") : ChatError()
    data class RefreshTokenRequired(override val message: String = "Refresh token required") : ChatError()
    data class RefreshTokenExpired(override val message: String = "Refresh token expired") : ChatError()
    data class InvalidRefreshToken(override val message: String = "Invalid refresh token") : ChatError()
    data class ProviderAlreadyLinked(override val message: String = "Provider already linked") : ChatError()
    data class InvalidLinkToken(override val message: String = "Invalid or expired link token") : ChatError()

    // User
    data class UserNotFound(val userId: String, override val message: String = "User not found: $userId") : ChatError()
    data class UsernameAlreadyTaken(val username: String, override val message: String = "Username already taken: $username") : ChatError()
    data class InvalidUsername(override val message: String = "Username must be 3-20 characters, lowercase letters, numbers, and underscores only") : ChatError()

    // Room
    data class RoomNotFound(val roomId: String, override val message: String = "Room not found: $roomId") : ChatError()
    data class RoomFull(val roomId: String, val maxMembers: Int, override val message: String = "Room is full (max $maxMembers)") : ChatError()
    data class NotRoomMember(val userId: String, val roomId: String, override val message: String = "Not a member of this chat room") : ChatError()
    data class DirectRoomAlreadyExists(val roomId: String, override val message: String = "Direct room already exists") : ChatError()

    // Message
    data class MessageNotFound(val messageId: String, override val message: String = "Message not found: $messageId") : ChatError()
    data class MessageTooLong(val maxLength: Int, override val message: String = "Message too long (max $maxLength)") : ChatError()
    data class EmptyMessage(override val message: String = "Message content cannot be empty") : ChatError()

    // WebSocket
    data class NotAuthenticated(override val message: String = "Not authenticated") : ChatError()
    data class RateLimited(override val message: String = "Reconnecting too fast") : ChatError()
    data class MaxConnectionsExceeded(val max: Int, override val message: String = "Max $max connections per user") : ChatError()

    // Generic
    data class Internal(override val message: String = "An unexpected error occurred") : ChatError()
}
