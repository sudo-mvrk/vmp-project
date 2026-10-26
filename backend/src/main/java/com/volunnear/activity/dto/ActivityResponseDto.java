package com.volunnear.activity.dto;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.dictionary.dto.CertificationResponseDto;
import com.volunnear.dictionary.dto.SkillResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param acceptedCount volunteers accepted so far; free spots = spotsNeeded - acceptedCount (Q77)
 * @param weights       the override, or null when the global weights apply
 */
public record ActivityResponseDto(
        Long id,
        Long organizationId,
        String title,
        @Schema(nullable = true) String shortDescription,
        @Schema(nullable = true) String fullDescription,
        Priority priority,
        ActivityStatus status,
        Double lat,
        Double lon,
        @Schema(nullable = true) String city,
        @Schema(nullable = true) String fullAddress,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int spotsNeeded,
        long acceptedCount,
        List<SkillResponseDto> requiredSkills,
        List<CertificationResponseDto> requiredCertifications,
        @Schema(nullable = true) ActivityWeightsDto weights
) {
}
