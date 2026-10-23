package com.volunnear.distribution.feasibility;

import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.AvailabilitySlot;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * The feasible set of docs/FUNCTION.md section 4: Available(v,t), Certified(v,t) and Cap(v) > 0.
 * Pure predicates over the profile records.
 */
public final class Feasibility {
    private Feasibility() {
    }

    public static boolean feasible(VolunteerProfile volunteer, ActivityProfile activity) {
        return available(volunteer, activity) && certified(volunteer, activity) && hasCapacity(volunteer);
    }

    /**
     * The weekly slots of the activity's day cover the whole activity window, and the volunteer is not
     * already accepted for an activity that overlaps it.
     */
    public static boolean available(VolunteerProfile volunteer, ActivityProfile activity) {
        TimeWindow window = activity.window();
        return slotsCover(volunteer.availability(), window)
                && volunteer.activeAssignments().stream().noneMatch(window::overlaps);
    }

    /**
     * Every certification the activity requires is held and still valid on the day the activity starts.
     * An activity that requires none is open to everyone.
     */
    public static boolean certified(VolunteerProfile volunteer, ActivityProfile activity) {
        LocalDate activityDay = activity.window().start().toLocalDate();
        return activity.requiredCertificationIds().stream().allMatch(requiredId ->
                volunteer.certifications().stream().anyMatch(held ->
                        held.certificationId() == requiredId && held.isValidOn(activityDay)));
    }

    public static boolean hasCapacity(VolunteerProfile volunteer) {
        return volunteer.remainingCapacity() > 0;
    }

    /**
     * Slots of the same day that touch or overlap count as one, so 09:00-12:00 and 12:00-15:00 cover
     * 10:00-14:00. Any gap inside the window fails.
     */
    private static boolean slotsCover(List<AvailabilitySlot> slots, TimeWindow window) {
        if (!window.isWithinOneDay()) {
            return false;
        }
        DayOfWeek day = window.start().getDayOfWeek();
        LocalTime end = window.end().toLocalTime();
        // walk the day's slots in order of start, pushing "covered up to" forward from the window start
        LocalTime coveredUntil = window.start().toLocalTime();
        List<AvailabilitySlot> daySlots = slots.stream()
                .filter(slot -> slot.dayOfWeek() == day)
                .sorted(Comparator.comparing(AvailabilitySlot::start))
                .toList();
        for (AvailabilitySlot slot : daySlots) {
            if (slot.start().isAfter(coveredUntil)) {
                return false; // a gap before this slot, and later slots start even later
            }
            if (slot.end().isAfter(coveredUntil)) {
                coveredUntil = slot.end();
            }
            if (!coveredUntil.isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
