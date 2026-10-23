package com.volunnear.distribution;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.AvailabilitySlot;
import com.volunnear.distribution.model.HeldCertification;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.GeoPoint;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test data for the distribution tests. The defaults describe a volunteer and an activity that match:
 * each test changes only the one thing it is about.
 */
public final class Profiles {
    /** 2026-11-09 is a Monday. */
    public static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    public static final GeoPoint PRAGUE = new GeoPoint(50.0755, 14.4378);

    private Profiles() {
    }

    public static TimeWindow window(LocalDate day, String from, String to) {
        return new TimeWindow(LocalDateTime.of(day, LocalTime.parse(from)), LocalDateTime.of(day, LocalTime.parse(to)));
    }

    public static AvailabilitySlot slot(DayOfWeek day, String from, String to) {
        return new AvailabilitySlot(day, LocalTime.parse(from), LocalTime.parse(to));
    }

    /** Available Monday 08:00-18:00, capacity 1, no assignments, in Prague. */
    public static VolunteerBuilder volunteer(long id) {
        return new VolunteerBuilder(id);
    }

    /** Monday 10:00-14:00 in Prague, OPEN, one free spot, MEDIUM priority, nothing required. */
    public static ActivityBuilder activity(long id) {
        return new ActivityBuilder(id);
    }

    public static final class VolunteerBuilder {
        private final long id;
        private GeoPoint location = PRAGUE;
        private final Map<Long, Double> skills = new HashMap<>();
        private final List<HeldCertification> certifications = new ArrayList<>();
        private List<AvailabilitySlot> availability = List.of(slot(DayOfWeek.MONDAY, "08:00", "18:00"));
        private final List<TimeWindow> activeAssignments = new ArrayList<>();
        private int maxActiveAssignments = 1;
        private final Set<Long> appliedActivityIds = new HashSet<>();
        private boolean banned;

        private VolunteerBuilder(long id) {
            this.id = id;
        }

        public VolunteerBuilder location(GeoPoint location) {
            this.location = location;
            return this;
        }

        public VolunteerBuilder skills(long... skillIds) {
            for (long skillId : skillIds) {
                skills.put(skillId, 1.0);
            }
            return this;
        }

        public VolunteerBuilder certification(long certificationId, LocalDate validUntil) {
            certifications.add(new HeldCertification(certificationId, validUntil));
            return this;
        }

        public VolunteerBuilder availability(AvailabilitySlot... slots) {
            this.availability = List.of(slots);
            return this;
        }

        public VolunteerBuilder acceptedFor(TimeWindow window) {
            activeAssignments.add(window);
            return this;
        }

        public VolunteerBuilder maxActiveAssignments(int maxActiveAssignments) {
            this.maxActiveAssignments = maxActiveAssignments;
            return this;
        }

        public VolunteerBuilder appliedTo(long activityId) {
            appliedActivityIds.add(activityId);
            return this;
        }

        public VolunteerBuilder banned() {
            this.banned = true;
            return this;
        }

        public VolunteerProfile build() {
            return new VolunteerProfile(id, location, skills, certifications, availability, activeAssignments,
                    maxActiveAssignments, appliedActivityIds, banned);
        }
    }

    public static final class ActivityBuilder {
        private final long id;
        private GeoPoint location = PRAGUE;
        private final Map<Long, Double> requiredSkills = new HashMap<>();
        private final Set<Long> requiredCertificationIds = new HashSet<>();
        private TimeWindow window = Profiles.window(MONDAY, "10:00", "14:00");
        private Priority priority = Priority.MEDIUM;
        private ActivityStatus status = ActivityStatus.OPEN;
        private int freeSpots = 1;

        private ActivityBuilder(long id) {
            this.id = id;
        }

        public ActivityBuilder location(GeoPoint location) {
            this.location = location;
            return this;
        }

        public ActivityBuilder requiredSkills(long... skillIds) {
            for (long skillId : skillIds) {
                requiredSkills.put(skillId, 1.0);
            }
            return this;
        }

        public ActivityBuilder requiredCertifications(long... certificationIds) {
            for (long certificationId : certificationIds) {
                requiredCertificationIds.add(certificationId);
            }
            return this;
        }

        public ActivityBuilder window(TimeWindow window) {
            this.window = window;
            return this;
        }

        public ActivityBuilder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public ActivityBuilder status(ActivityStatus status) {
            this.status = status;
            return this;
        }

        public ActivityBuilder freeSpots(int freeSpots) {
            this.freeSpots = freeSpots;
            return this;
        }

        public ActivityProfile build() {
            return new ActivityProfile(id, location, requiredSkills, requiredCertificationIds, window, priority,
                    status, freeSpots);
        }
    }
}
