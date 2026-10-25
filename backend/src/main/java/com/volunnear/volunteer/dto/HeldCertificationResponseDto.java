package com.volunnear.volunteer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record HeldCertificationResponseDto(Long certificationId, String name, @Schema(nullable = true) LocalDate validUntil) {
}
