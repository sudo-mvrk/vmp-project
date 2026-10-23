package com.volunnear.activity.dto;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.dictionary.dto.CertificationResponseDto;
import com.volunnear.dictionary.dto.SkillResponseDto;

import java.time.LocalDateTime;
import java.util.List;

/** @param weights the override, or null when the global weights apply */
public record ActivityResponseDto(
        Long id,
        Long organizationId,
        String title,
        String shortDescription,
        String fullDescription,
        Priority priority,
        ActivityStatus status,
        Double lat,
        Double lon,
        String city,
        String fullAddress,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int spotsNeeded,
        List<SkillResponseDto> requiredSkills,
        List<CertificationResponseDto> requiredCertifications,
        ActivityWeightsDto weights
) {
}
