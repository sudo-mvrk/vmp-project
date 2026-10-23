package com.volunnear.distribution.feasibility;

import com.volunnear.activity.ActivityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.volunnear.distribution.Profiles.activity;
import static com.volunnear.distribution.Profiles.volunteer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformEligibilityTest {

    @Test
    void openActivityWithAFreeSpotAndANewVolunteerIsEligible() {
        assertTrue(PlatformEligibility.platformEligible(volunteer(1).build(), activity(1).build()));
    }

    @ParameterizedTest
    @EnumSource(value = ActivityStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void activityThatIsNotOpenIsNotEligible(ActivityStatus status) {
        assertFalse(PlatformEligibility.platformEligible(volunteer(1).build(), activity(1).status(status).build()));
    }

    @Test
    void activityWithoutFreeSpotsIsNotEligible() {
        assertFalse(PlatformEligibility.platformEligible(volunteer(1).build(), activity(1).freeSpots(0).build()));
    }

    @Test
    void volunteerWhoAlreadyAppliedIsNotEligible() {
        assertFalse(PlatformEligibility.platformEligible(volunteer(1).appliedTo(1).build(), activity(1).build()));
    }

    @Test
    void applicationToAnotherActivityDoesNotMatter() {
        assertTrue(PlatformEligibility.platformEligible(volunteer(1).appliedTo(2).build(), activity(1).build()));
    }

    @Test
    void bannedVolunteerIsNotEligible() {
        assertFalse(PlatformEligibility.platformEligible(volunteer(1).banned().build(), activity(1).build()));
    }
}
