package com.volunnear.distribution.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A weekly availability slot of a volunteer: every week on this day, from start to end.
 */
public record AvailabilitySlot(DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
    public AvailabilitySlot {
        if (dayOfWeek == null || start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Slot needs a day and a start before its end, got " + dayOfWeek + " " + start + " - " + end);
        }
    }
}
