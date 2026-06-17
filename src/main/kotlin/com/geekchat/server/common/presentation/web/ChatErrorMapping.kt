package com.geekchat.server.common.presentation.web

import com.geekchat.server.common.error.ChatError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun ChatError.toResponseEntity(): ResponseEntity<ErrorResponse> {
    val status = when (this) {
        is ChatError.TokenNotProvided,
        is ChatError.TokenExpired,
        is ChatError.InvalidToken,
        is ChatError.RefreshTokenRequired,
        is ChatError.RefreshTokenExpired,
        is ChatError.InvalidRefreshToken,
        is ChatError.InvalidLinkToken,
        is ChatError.InvalidSignupToken,
        is ChatError.ProviderAlreadyLinked,
        is ChatError.NotAuthenticated,
        is ChatError.InvalidCredentials,
        -> HttpStatus.UNAUTHORIZED

        is ChatError.NotRoomMember,
        is ChatError.AccountWithdrawn,
        -> HttpStatus.FORBIDDEN

        is ChatError.UserNotFound,
        is ChatError.RoomNotFound,
        is ChatError.MessageNotFound,
        is ChatError.InviteLinkNotFound,
        -> HttpStatus.NOT_FOUND

        is ChatError.InviteLinkExpired,
        is ChatError.InviteLinkMaxUsesReached,
        -> HttpStatus.GONE

        is ChatError.UsernameAlreadyTaken,
        is ChatError.DirectRoomAlreadyExists,
        is ChatError.AlreadyRoomMember,
        is ChatError.EmailAlreadyInUse,
        -> HttpStatus.CONFLICT

        is ChatError.InvalidUsername,
        is ChatError.WeakPassword,
        is ChatError.NicknameRequired,
        is ChatError.MessageTooLong,
        is ChatError.EmptyMessage,
        is ChatError.RoomFull,
        is ChatError.RateLimited,
        is ChatError.MaxConnectionsExceeded,
        -> HttpStatus.BAD_REQUEST

        is ChatError.OAuthExchangeFailed,
        is ChatError.OAuthProfileMissingId,
        -> HttpStatus.BAD_GATEWAY

        is ChatError.Internal -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    return ResponseEntity.status(status).body(
        ErrorResponse(
            statusCode = status.value(),
            message = this.message,
            error = status.reasonPhrase,
        ),
    )
}
