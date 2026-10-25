package com.volunnear.volunteer.dto;

import com.volunnear.dictionary.dto.SkillResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** A volunteer profile without account data (Q3a); the owner sees it inside {@link OwnVolunteerProfileResponseDto}. */
public record VolunteerProfileResponseDto(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        @Schema(nullable = true) String bio,
        @Schema(nullable = true) Double lat,
        @Schema(nullable = true) Double lon,
        @Schema(nullable = true) String locationName,
        @Schema(nullable = true) String countryCode,
        @Schema(nullable = true) String city,
        @Schema(nullable = true) String region,
        @Schema(nullable = true) Integer radius,
        int maxActiveAssignments,
        List<SkillResponseDto> skills,
        List<HeldCertificationResponseDto> certifications,
        List<AvailabilitySlotDto> availability
) {
}
