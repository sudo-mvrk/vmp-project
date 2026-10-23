package com.volunnear.auth.dto;

import java.util.List;

public record AppUserResponseDto(
        Long id,
        String email,
        String username,
        List<String> roles
) {
}
