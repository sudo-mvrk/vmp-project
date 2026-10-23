package com.volunnear.volunteer.dto;

import java.time.LocalDate;

public record HeldCertificationResponseDto(Long certificationId, String name, LocalDate validUntil) {
}
