package com.volunnear.application.dto;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.application.ApplicationOrigin;
import com.volunnear.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** A volunteer's own application, with enough of the activity to list it. */
public record MyApplicationResponseDto(
        Long id,
        ApplicationStatus status,
        ApplicationOrigin origin,
        LocalDateTime appliedAt,
        @Schema(nullable = true) LocalDateTime decidedAt,
        Long activityId,
        String activityTitle,
        ActivityStatus activityStatus,
        LocalDateTime activityStartsAt,
        LocalDateTime activityEndsAt
) {
}
