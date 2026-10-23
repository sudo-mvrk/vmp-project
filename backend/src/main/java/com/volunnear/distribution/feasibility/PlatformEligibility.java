package com.volunnear.distribution.feasibility;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;

/**
 * Conditions that come from the platform and not from docs/FUNCTION.md: the activity is open and has a
 * free spot, the volunteer has no application for it yet and is not banned. Kept apart from
 * {@link Feasibility} so the two sets of rules stay recognisable.
 */
public final class PlatformEligibility {
    private PlatformEligibility() {
    }

    public static boolean platformEligible(VolunteerProfile volunteer, ActivityProfile activity) {
        return activity.status() == ActivityStatus.OPEN
                && activity.freeSpots() > 0
                && !volunteer.appliedActivityIds().contains(activity.id())
                && !volunteer.banned();
    }
}
