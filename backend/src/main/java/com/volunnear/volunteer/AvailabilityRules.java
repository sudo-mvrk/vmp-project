package com.volunnear.volunteer;

import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.volunteer.dto.AvailabilitySlotDto;

import java.util.Comparator;
import java.util.List;

/**
 * Input rules for weekly availability (Q3d): each slot starts before it ends, and slots of one day
 * do not overlap. Touching slots (09:00-12:00 and 12:00-15:00) are allowed; feasibility merges them (Q13a).
 */
final class AvailabilityRules {
    private AvailabilityRules() {
    }

    static void validate(List<AvailabilitySlotDto> slots) {
        for (AvailabilitySlotDto slot : slots) {
            if (!slot.start().isBefore(slot.end())) {
                throw new BadDataInRequestException("Slot on " + slot.dayOfWeek() + " must start before it ends: "
                        + slot.start() + " - " + slot.end());
            }
        }
        List<AvailabilitySlotDto> sorted = slots.stream()
                .sorted(Comparator.comparing(AvailabilitySlotDto::dayOfWeek).thenComparing(AvailabilitySlotDto::start))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            AvailabilitySlotDto previous = sorted.get(i - 1);
            AvailabilitySlotDto current = sorted.get(i);
            // Half-open windows: a slot may start exactly when the previous one ends.
            if (previous.dayOfWeek() == current.dayOfWeek() && current.start().isBefore(previous.end())) {
                throw new BadDataInRequestException("Slots on " + current.dayOfWeek() + " overlap: "
                        + previous.start() + " - " + previous.end() + " and " + current.start() + " - " + current.end());
            }
        }
    }
}
