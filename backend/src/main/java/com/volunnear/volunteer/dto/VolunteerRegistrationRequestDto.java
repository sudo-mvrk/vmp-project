package com.volunnear.volunteer.dto;

import com.volunnear.auth.dto.RegistrationCredentials;
import jakarta.validation.constraints.*;

import java.time.LocalDate;


public record VolunteerRegistrationRequestDto(
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
        String firstName,
        @NotBlank
        String lastName,
        @NotNull(message = "Date of birth shouldn't be empty")
        @Past(message = "Date of birth should be in the past")
        LocalDate dateOfBirth
) implements RegistrationCredentials {
}
