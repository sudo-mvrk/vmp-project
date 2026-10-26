package com.volunnear.distribution.adapter;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.volunteer.Volunteer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads what the profile records need beyond the entities themselves (active assignments, applied activities,
 * accepted counts) and maps them with {@link ProfileAdapter}. Used by recommendations and invitations, so both
 * judge a volunteer and an activity from the same data. Call it inside a transaction.
 */
@Component
@RequiredArgsConstructor
public class ProfileLoader {
    private final ActivityRepository activityRepository;
    private final ApplicationRepository applicationRepository;

    /** Three queries for any number of volunteers: the volunteers' own collections are batch-fetched. */
    public List<VolunteerProfile> volunteerProfiles(Collection<Volunteer> volunteers) {
        if (volunteers.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = volunteers.stream().map(Volunteer::getId).collect(Collectors.toSet());
        Map<Long, List<Activity>> active = applicationRepository.findActiveAssignmentsOfVolunteers(ids).stream()
                .collect(Collectors.groupingBy(application -> application.getVolunteer().getId(),
                        Collectors.mapping(application -> application.getActivity(), Collectors.toList())));
        Map<Long, Set<Long>> applied = applicationRepository.findAppliedActivities(ids).stream()
                .collect(Collectors.groupingBy(ApplicationRepository.AppliedActivity::getVolunteerId,
                        Collectors.mapping(ApplicationRepository.AppliedActivity::getActivityId, Collectors.toSet())));
        return volunteers.stream()
                .map(volunteer -> ProfileAdapter.toVolunteerProfile(volunteer,
                        active.getOrDefault(volunteer.getId(), List.of()),
                        applied.getOrDefault(volunteer.getId(), Set.of())))
                .toList();
    }

    public VolunteerProfile volunteerProfile(Volunteer volunteer) {
        return volunteerProfiles(List.of(volunteer)).get(0);
    }

    public ActivityProfile activityProfile(Activity activity) {
        return ProfileAdapter.toActivityProfile(activity, activityRepository.countAccepted(activity.getId()));
    }

    /** One query for the accepted counts of all activities. */
    public List<ActivityProfile> activityProfiles(Collection<Activity> activities) {
        if (activities.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> accepted = activityRepository.acceptedCounts(activities.stream().map(Activity::getId).toList());
        return activities.stream()
                .map(activity -> ProfileAdapter.toActivityProfile(activity, accepted.getOrDefault(activity.getId(), 0L)))
                .toList();
    }
}
