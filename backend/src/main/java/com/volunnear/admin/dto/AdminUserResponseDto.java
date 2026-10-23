package com.volunnear.admin.dto;

import com.volunnear.auth.Role;

import java.time.LocalDateTime;
import java.util.Set;

/** A user account as an admin sees it (Q29). */
public record AdminUserResponseDto(
        Long id,
        String username,
        String email,
        Set<Role> roles,
        boolean banned,
        LocalDateTime createdAt
) {
}
