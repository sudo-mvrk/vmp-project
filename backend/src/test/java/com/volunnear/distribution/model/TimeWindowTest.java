package com.volunnear.distribution.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.volunnear.distribution.Profiles.MONDAY;
import static com.volunnear.distribution.Profiles.window;
import static org.junit.jupiter.api.Assertions.*;

class TimeWindowTest {

    @Test
    void windowsThatShareTimeOverlap() {
        assertTrue(window(MONDAY, "10:00", "14:00").overlaps(window(MONDAY, "13:00", "15:00")));
        assertTrue(window(MONDAY, "10:00", "14:00").overlaps(window(MONDAY, "11:00", "12:00")));
        assertTrue(window(MONDAY, "11:00", "12:00").overlaps(window(MONDAY, "10:00", "14:00")));
    }

    @Test
    void windowsThatOnlyTouchDoNotOverlap() {
        assertFalse(window(MONDAY, "10:00", "12:00").overlaps(window(MONDAY, "12:00", "14:00")));
        assertFalse(window(MONDAY, "12:00", "14:00").overlaps(window(MONDAY, "10:00", "12:00")));
    }

    @Test
    void windowsOnDifferentDaysDoNotOverlap() {
        assertFalse(window(MONDAY, "10:00", "14:00").overlaps(window(MONDAY.plusDays(1), "10:00", "14:00")));
    }

    @Test
    void rejectsStartThatIsNotBeforeEnd() {
        LocalDateTime noon = MONDAY.atTime(12, 0);

        assertThrows(IllegalArgumentException.class, () -> new TimeWindow(noon, noon));
        assertThrows(IllegalArgumentException.class, () -> new TimeWindow(noon, noon.minusHours(1)));
    }

    @Test
    void windowThatCrossesMidnightIsNotWithinOneDay() {
        assertTrue(window(MONDAY, "10:00", "14:00").isWithinOneDay());
        assertFalse(new TimeWindow(MONDAY.atTime(22, 0), MONDAY.plusDays(1).atTime(2, 0)).isWithinOneDay());
    }
}
