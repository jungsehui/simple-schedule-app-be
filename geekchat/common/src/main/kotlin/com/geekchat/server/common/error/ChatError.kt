package com.geekchat.server.common.error

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
    data class WeakPassword(val reason: String = "Password must be at least 8 characters with letters and numbers", override val message: String = reason) : ChatError()
    data class InvalidCredentials(override val message: String = "Invalid username or password") : ChatError()
    data class NicknameRequired(override val message: String = "Nickname is required") : ChatError()
    data class EmailAlreadyInUse(val email: String, override val message: String = "Email already in use: $email") : ChatError()
    data class AccountWithdrawn(override val message: String = "Account has been withdrawn") : ChatError()

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

    // InviteLink
    data class InviteLinkNotFound(val code: String, override val message: String = "Invite link not found: $code") : ChatError()
    data class InviteLinkExpired(val code: String, override val message: String = "Invite link has expired") : ChatError()
    data class InviteLinkMaxUsesReached(val code: String, override val message: String = "Invite link has reached maximum uses") : ChatError()
    data class AlreadyRoomMember(val userId: String, val roomId: String, override val message: String = "Already a member of this room") : ChatError()

    // OAuth
    data class OAuthExchangeFailed(val provider: String, val reason: String, override val message: String = "OAuth exchange failed for $provider: $reason") : ChatError()
    data class OAuthProfileMissingId(val provider: String, override val message: String = "OAuth profile from $provider is missing required id") : ChatError()
    data class InvalidSignupToken(override val message: String = "Invalid or expired signup token") : ChatError()

    // Generic
    data class Internal(override val message: String = "An unexpected error occurred") : ChatError()
}
