package com.volunnear.volunteer.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A weekly slot, in requests and responses. The latest possible end is 23:59 (Q3d). */
public record AvailabilitySlotDto(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime start,
        @NotNull LocalTime end
) {
}
