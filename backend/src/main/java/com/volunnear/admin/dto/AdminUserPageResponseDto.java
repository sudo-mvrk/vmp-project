package com.volunnear.admin.dto;

import java.util.List;

/** One page of user accounts, shaped like the activity page (Q29). */
public record AdminUserPageResponseDto(
        List<AdminUserResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
