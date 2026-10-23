package com.volunnear.organization.dto;

import com.volunnear.auth.dto.RegistrationCredentials;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRegistrationRequestDto(
        @Size(min = 3, max = 25)
        @NotBlank
        String username,
        @Size(min = 4, max = 35)
        @NotBlank
        String password,
        @Email
        @NotBlank
        String email,
        @NotBlank
        String organizationName
) implements RegistrationCredentials {
}
