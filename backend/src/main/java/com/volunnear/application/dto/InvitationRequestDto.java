package com.volunnear.application.dto;

import jakarta.validation.constraints.NotNull;

/** Q5m: the volunteer to invite, usually taken from the recommendation list (Q19). */
public record InvitationRequestDto(@NotNull Long volunteerId) {
}
