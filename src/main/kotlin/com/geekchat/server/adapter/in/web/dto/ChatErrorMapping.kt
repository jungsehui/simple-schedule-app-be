package com.geekchat.server.adapter.`in`.web.dto

import com.geekchat.server.domain.error.ChatError
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
        is ChatError.ProviderAlreadyLinked,
        is ChatError.NotAuthenticated,
        -> HttpStatus.UNAUTHORIZED

        is ChatError.NotRoomMember -> HttpStatus.FORBIDDEN

        is ChatError.UserNotFound,
        is ChatError.RoomNotFound,
        is ChatError.MessageNotFound,
        -> HttpStatus.NOT_FOUND

        is ChatError.UsernameAlreadyTaken,
        is ChatError.DirectRoomAlreadyExists,
        -> HttpStatus.CONFLICT

        is ChatError.InvalidUsername,
        is ChatError.MessageTooLong,
        is ChatError.EmptyMessage,
        is ChatError.RoomFull,
        is ChatError.RateLimited,
        is ChatError.MaxConnectionsExceeded,
        -> HttpStatus.BAD_REQUEST

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
