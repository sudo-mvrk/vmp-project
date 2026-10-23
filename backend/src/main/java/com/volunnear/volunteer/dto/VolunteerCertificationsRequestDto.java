package com.volunnear.volunteer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The complete list of certifications (Q3c); an empty list clears them. */
public record VolunteerCertificationsRequestDto(@NotNull List<@NotNull @Valid HeldCertificationRequestDto> certifications) {
}
