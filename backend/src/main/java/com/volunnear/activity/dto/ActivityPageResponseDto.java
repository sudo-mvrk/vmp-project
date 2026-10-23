package com.volunnear.activity.dto;

import java.util.List;

/** One page of activities. Our own record: Spring's PageImpl has no stable JSON shape. */
public record ActivityPageResponseDto(
        List<ActivityResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
