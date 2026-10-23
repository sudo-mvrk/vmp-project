package com.volunnear.distribution.adapter;

import com.volunnear.activity.Activity;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.Skill;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.AvailabilitySlot;
import com.volunnear.distribution.model.HeldCertification;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.GeoPoint;
import com.volunnear.volunteer.Volunteer;
import org.locationtech.jts.geom.Point;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps entities to the plain records of the distribution. The only place where the scoring code meets JPA:
 * the caller loads what the records need, this class only copies it.
 * Times need no conversion: entities already hold local times of the one configured zone (Q4a).
 */
public final class ProfileAdapter {
    /** Skills are binary in the MVP (Q7): a skill that is held or required has weight 1. */
    static final double SKILL_PRESENT = 1.0;

    private ProfileAdapter() {
    }

    /**
     * @param activeAssignments  the activities of the volunteer's accepted applications that are neither completed
     *                           nor cancelled (Q16), as returned by {@code findActiveAssignments}
     * @param appliedActivityIds every activity the volunteer has an application for, in any status (Q5b)
     */
    public static VolunteerProfile toVolunteerProfile(Volunteer volunteer,
                                                      Collection<Activity> activeAssignments,
                                                      Set<Long> appliedActivityIds) {
        return new VolunteerProfile(
                volunteer.getId(),
                toGeoPoint(volunteer.getLocation()),
                skillVector(volunteer.getSkills()),
                volunteer.getCertifications().stream()
                        .map(held -> new HeldCertification(held.getCertification().getId(), held.getValidUntil()))
                        .toList(),
                volunteer.getAvailability().stream()
                        .map(slot -> new AvailabilitySlot(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()))
                        .toList(),
                activeAssignments.stream().map(ProfileAdapter::windowOf).toList(),
                volunteer.getMaxActiveAssignments(),
                appliedActivityIds,
                volunteer.getAppUser().isBanned());
    }

    /** @param acceptedCount accepted applications of the activity; free spots = spots needed minus this */
    public static ActivityProfile toActivityProfile(Activity activity, long acceptedCount) {
        return new ActivityProfile(
                activity.getId(),
                toGeoPoint(activity.getLocation()),
                skillVector(activity.getRequiredSkills()),
                activity.getRequiredCertifications().stream().map(Certification::getId).collect(Collectors.toSet()),
                windowOf(activity),
                // the enum as it is; its numeric value lives only in ScoringFunction.priorityValue
                activity.getPriority(),
                activity.getStatus(),
                Math.toIntExact(activity.getSpotsNeeded() - acceptedCount));
    }

    /** A JTS point stores x = longitude, y = latitude. A missing point stays missing (Q5: u_geo = 0). */
    public static GeoPoint toGeoPoint(Point point) {
        return point == null ? null : new GeoPoint(point.getY(), point.getX());
    }

    public static TimeWindow windowOf(Activity activity) {
        return new TimeWindow(activity.getStartsAt(), activity.getEndsAt());
    }

    private static Map<Long, Double> skillVector(Set<Skill> skills) {
        return skills.stream().collect(Collectors.toMap(Skill::getId, skill -> SKILL_PRESENT));
    }
}
