package com.volunnear.volunteer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The complete weekly availability (Q3c); an empty list clears it. */
public record VolunteerAvailabilityRequestDto(@NotNull List<@NotNull @Valid AvailabilitySlotDto> slots) {
}
