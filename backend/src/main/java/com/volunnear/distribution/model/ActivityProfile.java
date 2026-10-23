package com.volunnear.distribution.model;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.distribution.scoring.GeoPoint;

import java.util.Map;
import java.util.Set;

/**
 * Everything the distribution needs to know about an activity. A plain record, like {@link VolunteerProfile}.
 *
 * @param requiredSkills skill id to weight (1.0 for a required skill)
 * @param freeSpots      spots needed minus accepted volunteers
 */
public record ActivityProfile(long id,
                              GeoPoint location,
                              Map<Long, Double> requiredSkills,
                              Set<Long> requiredCertificationIds,
                              TimeWindow window,
                              Priority priority,
                              ActivityStatus status,
                              int freeSpots) {
    public ActivityProfile {
        if (window == null || priority == null || status == null) {
            throw new IllegalArgumentException("An activity needs a time window, a priority and a status");
        }
        requiredSkills = Map.copyOf(requiredSkills);
        requiredCertificationIds = Set.copyOf(requiredCertificationIds);
    }
}
