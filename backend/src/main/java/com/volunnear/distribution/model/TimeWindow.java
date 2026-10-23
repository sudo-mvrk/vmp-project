package com.volunnear.distribution.model;

import java.time.LocalDateTime;

/**
 * A period of time [start, end) in the local time of the configured zone.
 * The end is exclusive: a window that starts at the minute another one ends does not overlap it.
 */
public record TimeWindow(LocalDateTime start, LocalDateTime end) {
    public TimeWindow {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Window start must be before its end, got " + start + " - " + end);
        }
    }

    public boolean overlaps(TimeWindow other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Multi-day activities are out of scope; such a window is never covered by weekly slots. */
    public boolean isWithinOneDay() {
        return start.toLocalDate().equals(end.toLocalDate());
    }
}
