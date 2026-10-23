package com.volunnear.application.dto;

import java.time.LocalDateTime;

/** One completed activity the volunteer took part in (Q8e). */
public record HistoryItemResponseDto(
        Long applicationId,
        Long activityId,
        String title,
        String city,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long organizationId,
        String organizationName
) {
}
