package com.geekchat.server.room.presentation.web

import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Component

/**
 * Centralizes the hypermedia links for a single room representation (Phase 7).
 *
 * Wrapping is intentionally **additive and backward-compatible**: [EntityModel] unwraps the wrapped
 * response's fields at the JSON top level and only adds a `_links` object, so the existing REST
 * contract (and the Next.js frontend / integration tests) is preserved — a single resource stays
 * an object, just with `_links` added.
 *
 * Applied to single-resource responses only (create room, join by invite). Collection endpoints
 * (GET /api/rooms) deliberately stay a plain top-level array: rendering per-element links would
 * require `CollectionModel`, which wraps the array in a `_embedded` object and breaks the array
 * contract. (Spring HATEOAS's HAL message converter is type-constrained to `RepresentationModel`,
 * so a bare `List<EntityModel<…>>` does not render `_links` anyway — verified empirically.)
 *
 * Links point only to controllers in this (:room) module — cross-module links (e.g. messages in
 * :chat) are deliberately omitted to respect the modular-monolith boundary.
 */
@Component
class RoomModelAssembler {

    fun toModel(room: CreateRoomResponse): EntityModel<CreateRoomResponse> =
        EntityModel.of(room).also { model ->
            // self → /api/rooms/{id} (resource identity; getRooms() supplies the /api/rooms base)
            model.add(linkTo(methodOn(RoomController::class.java).getRooms("")).slash(room.id).withSelfRel())
            // mute → PATCH /api/rooms/{id}/mute
            model.add(linkTo(methodOn(RoomController::class.java).setMute("", room.id, MuteRoomRequest())).withRel("mute"))
            // invite-link → POST /api/rooms/{id}/invite-link
            model.add(
                linkTo(methodOn(InviteLinkController::class.java).createInviteLink("", room.id, CreateInviteLinkRequest()))
                    .withRel("invite-link"),
            )
        }
}
