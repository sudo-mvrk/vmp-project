package com.volunnear.evaluation;

import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.ScoringFunction;

/**
 * A volunteer of the simulation: the profile the distribution works on, plus the radius, which the platform
 * applies as a database prefilter (Q6) and which is therefore not part of {@link VolunteerProfile}.
 *
 * @param radiusKm null when the volunteer has no radius: every activity is in reach
 */
record SimVolunteer(VolunteerProfile profile, Integer radiusKm) {
    long id() {
        return profile.id();
    }

    /**
     * The radius prefilter of the recommendation endpoint, with the haversine distance instead of PostGIS
     * {@code ST_DWithin} (METHOD.md section 8). Without a location or a radius every activity is in reach;
     * with both, an activity without a location is not (its geography is null in the database).
     */
    boolean reaches(ActivityProfile activity) {
        if (profile.location() == null || radiusKm == null) {
            return true;
        }
        return activity.location() != null
                && ScoringFunction.haversineKm(profile.location(), activity.location()) <= radiusKm;
    }
}
