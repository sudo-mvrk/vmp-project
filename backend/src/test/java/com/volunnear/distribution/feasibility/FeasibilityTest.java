package com.volunnear.distribution.feasibility;

import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.volunnear.distribution.Profiles.*;
import static org.junit.jupiter.api.Assertions.*;

class FeasibilityTest {
    /** Monday 10:00-14:00, nothing required. */
    private final ActivityProfile activity = activity(1).build();

    @Test
    void theTestDayIsAMonday() {
        assertEquals(DayOfWeek.MONDAY, MONDAY.getDayOfWeek());
    }

    @Nested
    class Available {
        @Test
        void slotThatContainsTheWindowIsAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(slot(DayOfWeek.MONDAY, "08:00", "18:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotThatEndsExactlyAtTheActivityEndIsAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(slot(DayOfWeek.MONDAY, "10:00", "14:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotThatEndsOneMinuteEarlyIsNotAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(slot(DayOfWeek.MONDAY, "08:00", "13:59")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotThatStartsOneMinuteLateIsNotAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(slot(DayOfWeek.MONDAY, "10:01", "18:00")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotOnAnotherDayIsNotAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(slot(DayOfWeek.TUESDAY, "08:00", "18:00")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void volunteerWithoutSlotsIsNotAvailable() {
            assertFalse(Feasibility.available(volunteer(1).availability().build(), activity));
        }

        @Test
        void slotsThatTouchCombine() {
            VolunteerProfile volunteer = volunteer(1).availability(
                    slot(DayOfWeek.MONDAY, "12:00", "15:00"),   // given out of order on purpose
                    slot(DayOfWeek.MONDAY, "09:00", "12:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotsThatOverlapCombine() {
            VolunteerProfile volunteer = volunteer(1).availability(
                    slot(DayOfWeek.MONDAY, "09:00", "13:00"),
                    slot(DayOfWeek.MONDAY, "11:00", "15:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void gapBetweenSlotsInsideTheWindowIsNotAvailable() {
            VolunteerProfile volunteer = volunteer(1).availability(
                    slot(DayOfWeek.MONDAY, "09:00", "12:00"),
                    slot(DayOfWeek.MONDAY, "12:01", "15:00")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void slotsOfDifferentDaysDoNotCombine() {
            VolunteerProfile volunteer = volunteer(1).availability(
                    slot(DayOfWeek.MONDAY, "09:00", "12:00"),
                    slot(DayOfWeek.TUESDAY, "12:00", "15:00")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void acceptedActivityThatOverlapsMakesTheVolunteerUnavailable() {
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(5)
                    .acceptedFor(window(MONDAY, "13:00", "16:00")).build();

            assertFalse(Feasibility.available(volunteer, activity));
        }

        @Test
        void acceptedActivityThatEndsWhenThisOneStartsDoesNotBlock() {
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(5)
                    .acceptedFor(window(MONDAY, "08:00", "10:00"))
                    .acceptedFor(window(MONDAY, "14:00", "16:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void acceptedActivityOnTheSameWeekdayOfAnotherWeekDoesNotBlock() {
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(5)
                    .acceptedFor(window(MONDAY.plusWeeks(1), "10:00", "14:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
        }

        @Test
        void activityThatCrossesMidnightIsNeverCovered() {
            ActivityProfile overnight = activity(1)
                    .window(new TimeWindow(MONDAY.atTime(22, 0), MONDAY.plusDays(1).atTime(2, 0))).build();
            VolunteerProfile volunteer = volunteer(1).availability(
                    slot(DayOfWeek.MONDAY, "00:00", "23:59"),
                    slot(DayOfWeek.TUESDAY, "00:00", "23:59")).build();

            assertFalse(Feasibility.available(volunteer, overnight));
        }
    }

    @Nested
    class Certified {
        private static final long FIRST_AID = 10;
        private static final long DRIVING_LICENCE = 11;
        private final ActivityProfile needsFirstAid = activity(1).requiredCertifications(FIRST_AID).build();
        private final LocalDate activityDay = MONDAY;

        @Test
        void activityWithoutRequiredCertificationsAcceptsEveryone() {
            assertTrue(Feasibility.certified(volunteer(1).build(), activity));
        }

        @Test
        void heldCertificationThatDoesNotExpireIsCertified() {
            VolunteerProfile volunteer = volunteer(1).certification(FIRST_AID, null).build();

            assertTrue(Feasibility.certified(volunteer, needsFirstAid));
        }

        @Test
        void missingCertificationIsNotCertified() {
            VolunteerProfile volunteer = volunteer(1).certification(DRIVING_LICENCE, null).build();

            assertFalse(Feasibility.certified(volunteer, needsFirstAid));
        }

        @Test
        void certificationThatExpiresOnTheActivityDayIsStillValid() {
            VolunteerProfile volunteer = volunteer(1).certification(FIRST_AID, activityDay).build();

            assertTrue(Feasibility.certified(volunteer, needsFirstAid));
        }

        @Test
        void certificationThatExpiredTheDayBeforeIsNotCertified() {
            VolunteerProfile volunteer = volunteer(1).certification(FIRST_AID, activityDay.minusDays(1)).build();

            assertFalse(Feasibility.certified(volunteer, needsFirstAid));
        }

        @Test
        void everyRequiredCertificationMustBeHeld() {
            ActivityProfile needsBoth = activity(1).requiredCertifications(FIRST_AID, DRIVING_LICENCE).build();

            assertFalse(Feasibility.certified(volunteer(1).certification(FIRST_AID, null).build(), needsBoth));
            assertTrue(Feasibility.certified(
                    volunteer(1).certification(FIRST_AID, null).certification(DRIVING_LICENCE, null).build(), needsBoth));
        }

        @Test
        void renewedCertificationCountsWhenAnOlderEntryHasExpired() {
            VolunteerProfile volunteer = volunteer(1)
                    .certification(FIRST_AID, activityDay.minusYears(1))
                    .certification(FIRST_AID, activityDay.plusYears(1)).build();

            assertTrue(Feasibility.certified(volunteer, needsFirstAid));
        }
    }

    @Nested
    class Capacity {
        private final TimeWindow anotherDay = window(MONDAY.plusDays(2), "10:00", "14:00");

        @Test
        void remainingCapacityOfOneHasCapacity() {
            // max 2, accepted for 1 -> Cap(v) = 1
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(2).acceptedFor(anotherDay).build();

            assertEquals(1, volunteer.remainingCapacity());
            assertTrue(Feasibility.hasCapacity(volunteer));
        }

        @Test
        void remainingCapacityOfZeroHasNoCapacity() {
            // max 1, accepted for 1 -> Cap(v) = 0
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(1).acceptedFor(anotherDay).build();

            assertEquals(0, volunteer.remainingCapacity());
            assertFalse(Feasibility.hasCapacity(volunteer));
        }

        @Test
        void maximumOfZeroNeverHasCapacity() {
            assertFalse(Feasibility.hasCapacity(volunteer(1).maxActiveAssignments(0).build()));
        }
    }

    @Nested
    class Feasible {
        @Test
        void volunteerThatPassesEveryPredicateIsFeasible() {
            assertTrue(Feasibility.feasible(volunteer(1).build(), activity));
        }

        @Test
        void failingAvailabilityAloneMakesTheVolunteerInfeasible() {
            assertFalse(Feasibility.feasible(volunteer(1).availability().build(), activity));
        }

        @Test
        void failingCertificationAloneMakesTheVolunteerInfeasible() {
            assertFalse(Feasibility.feasible(volunteer(1).build(), activity(1).requiredCertifications(10).build()));
        }

        @Test
        void failingCapacityAloneMakesTheVolunteerInfeasible() {
            // the accepted activity is on another day, so only capacity fails
            VolunteerProfile volunteer = volunteer(1).maxActiveAssignments(1)
                    .acceptedFor(window(MONDAY.plusDays(2), "10:00", "14:00")).build();

            assertTrue(Feasibility.available(volunteer, activity));
            assertFalse(Feasibility.feasible(volunteer, activity));
        }
    }
}
