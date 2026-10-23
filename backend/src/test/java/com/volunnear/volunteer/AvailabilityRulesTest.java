package com.volunnear.volunteer;

import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.volunteer.dto.AvailabilitySlotDto;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityRulesTest {

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> AvailabilityRules.validate(List.of(slot(MONDAY, "12:00", "09:00"))))
                .isInstanceOf(BadDataInRequestException.class)
                .hasMessageContaining("start before it ends");
    }

    @Test
    void emptySlotIsRejected() {
        assertThatThrownBy(() -> AvailabilityRules.validate(List.of(slot(MONDAY, "09:00", "09:00"))))
                .isInstanceOf(BadDataInRequestException.class);
    }

    @Test
    void overlappingSlotsOfOneDayAreRejectedInAnyOrder() {
        assertThatThrownBy(() -> AvailabilityRules.validate(List.of(
                slot(MONDAY, "11:00", "15:00"), slot(MONDAY, "09:00", "12:00"))))
                .isInstanceOf(BadDataInRequestException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void slotInsideAnotherIsRejected() {
        assertThatThrownBy(() -> AvailabilityRules.validate(List.of(
                slot(MONDAY, "08:00", "18:00"), slot(MONDAY, "10:00", "11:00"))))
                .isInstanceOf(BadDataInRequestException.class);
    }

    @Test
    void touchingSlotsAreAllowed() {
        // Q13a: half-open windows, so 09-12 and 12-15 only touch.
        assertThatCode(() -> AvailabilityRules.validate(List.of(
                slot(MONDAY, "09:00", "12:00"), slot(MONDAY, "12:00", "15:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void sameHoursOnDifferentDaysAreAllowed() {
        assertThatCode(() -> AvailabilityRules.validate(List.of(
                slot(MONDAY, "09:00", "12:00"), slot(TUESDAY, "09:00", "12:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void latestEndIs2359() {
        // Q3d: LocalTime has no 24:00, so a full day is 00:00 - 23:59.
        assertThatCode(() -> AvailabilityRules.validate(List.of(slot(MONDAY, "00:00", "23:59"))))
                .doesNotThrowAnyException();
    }

    private static AvailabilitySlotDto slot(java.time.DayOfWeek day, String start, String end) {
        return new AvailabilitySlotDto(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
