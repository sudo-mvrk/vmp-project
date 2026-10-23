package com.volunnear.distribution.model;

import com.volunnear.distribution.scoring.GeoPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the distribution needs to know about a volunteer. A plain record: it is filled from the
 * entities by an adapter and never touches the database.
 *
 * @param location             may be null: the volunteer is still listed, with u_geo = 0
 * @param skills               skill id to weight (1.0 for a skill the volunteer has)
 * @param activeAssignments    time windows of the activities the volunteer is accepted for and that are
 *                             neither completed nor cancelled; their number is the used capacity
 * @param maxActiveAssignments how many such activities the volunteer takes at the same time
 * @param appliedActivityIds   activities the volunteer already has an application or invitation for
 */
public record VolunteerProfile(long id,
                               GeoPoint location,
                               Map<Long, Double> skills,
                               List<HeldCertification> certifications,
                               List<AvailabilitySlot> availability,
                               List<TimeWindow> activeAssignments,
                               int maxActiveAssignments,
                               Set<Long> appliedActivityIds,
                               boolean banned) {
    public VolunteerProfile {
        if (maxActiveAssignments < 0) {
            throw new IllegalArgumentException("maxActiveAssignments must not be negative, got " + maxActiveAssignments);
        }
        skills = Map.copyOf(skills);
        certifications = List.copyOf(certifications);
        availability = List.copyOf(availability);
        activeAssignments = List.copyOf(activeAssignments);
        appliedActivityIds = Set.copyOf(appliedActivityIds);
    }

    /** Cap(v): the capacity that is still free. It is calculated, never stored. */
    public int remainingCapacity() {
        return maxActiveAssignments - activeAssignments.size();
    }
}
