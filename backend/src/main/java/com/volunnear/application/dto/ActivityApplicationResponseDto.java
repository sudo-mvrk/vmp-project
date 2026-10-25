package com.volunnear.application.dto;

import com.volunnear.application.ApplicationOrigin;
import com.volunnear.application.ApplicationStatus;
import com.volunnear.volunteer.dto.VolunteerProfileResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * An application as the activity's organization sees it: the volunteer without account data (Q3a, Q5f).
 * {@code volunteerEmail} is set only for an accepted application, otherwise null (Q7d).
 */
public record ActivityApplicationResponseDto(
        Long id,
        ApplicationStatus status,
        ApplicationOrigin origin,
        LocalDateTime appliedAt,
        @Schema(nullable = true) LocalDateTime decidedAt,
        Long activityId,
        VolunteerProfileResponseDto volunteer,
        @Schema(nullable = true) String volunteerEmail
) {
}
