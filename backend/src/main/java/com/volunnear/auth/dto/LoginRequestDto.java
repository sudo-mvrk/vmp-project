package com.volunnear.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank (message = "Login or email can't be empty") String username,
        @NotBlank String password
) {
}
