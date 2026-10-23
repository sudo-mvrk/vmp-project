package com.volunnear.volunteer.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** validUntil is optional (no expiry) and inclusive; a date in the past is rejected (Q3f). */
public record HeldCertificationRequestDto(
        @NotNull Long certificationId,
        @FutureOrPresent(message = "validUntil must not be in the past") LocalDate validUntil
) {
}
