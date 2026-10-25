package com.volunnear.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** One completed activity the volunteer took part in (Q8e). */
public record HistoryItemResponseDto(
        Long applicationId,
        Long activityId,
        String title,
        @Schema(nullable = true) String city,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long organizationId,
        String organizationName
) {
}
