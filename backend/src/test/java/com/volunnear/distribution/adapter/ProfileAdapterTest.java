package com.volunnear.distribution.adapter;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.auth.AppUser;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.Skill;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.AvailabilitySlot;
import com.volunnear.distribution.model.HeldCertification;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.GeoPoint;
import com.volunnear.volunteer.Volunteer;
import com.volunnear.volunteer.VolunteerAvailability;
import com.volunnear.volunteer.VolunteerCertification;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAdapterTest {
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 9, 10, 0);

    @Test
    void volunteerIsMappedWithPointXAsLongitude() {
        Volunteer volunteer = Volunteer.builder()
                .id(7L)
                .appUser(AppUser.builder().id(7L).isBanned(true).build())
                // JTS: x = longitude 14.42, y = latitude 50.08
                .location(GEOMETRY.createPoint(new Coordinate(14.42, 50.08)))
                .maxActiveAssignments(3)
                .skills(Set.of(skill(1), skill(2)))
                .certifications(List.of(new VolunteerCertification(certification(5), LocalDate.of(2027, 1, 31))))
                .availability(List.of(new VolunteerAvailability(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0))))
                .build();
        Activity assigned = activity(30, 4);

        VolunteerProfile profile = ProfileAdapter.toVolunteerProfile(volunteer, List.of(assigned), Set.of(30L, 31L));

        assertThat(profile.id()).isEqualTo(7);
        assertThat(profile.location()).isEqualTo(new GeoPoint(50.08, 14.42));
        assertThat(profile.skills()).isEqualTo(Map.of(1L, 1.0, 2L, 1.0));
        assertThat(profile.certifications()).containsExactly(new HeldCertification(5, LocalDate.of(2027, 1, 31)));
        assertThat(profile.availability())
                .containsExactly(new AvailabilitySlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        assertThat(profile.activeAssignments()).containsExactly(new TimeWindow(START, START.plusHours(4)));
        assertThat(profile.maxActiveAssignments()).isEqualTo(3);
        assertThat(profile.remainingCapacity()).isEqualTo(2);
        assertThat(profile.appliedActivityIds()).containsExactlyInAnyOrder(30L, 31L);
        assertThat(profile.banned()).isTrue();
    }

    @Test
    void volunteerWithoutLocationOrSkillsKeepsThemEmpty() {
        Volunteer volunteer = Volunteer.builder().id(8L).appUser(AppUser.builder().id(8L).build()).build();

        VolunteerProfile profile = ProfileAdapter.toVolunteerProfile(volunteer, List.of(), Set.of());

        assertThat(profile.location()).isNull();
        assertThat(profile.skills()).isEmpty();
        assertThat(profile.remainingCapacity()).isEqualTo(1);
        assertThat(profile.banned()).isFalse();
    }

    @Test
    void activityIsMappedWithFreeSpots() {
        Activity activity = activity(40, 2);
        activity.setPriority(Priority.URGENT);
        activity.setSpotsNeeded(5);
        activity.getRequiredSkills().add(skill(3));
        activity.getRequiredCertifications().add(certification(6));
        activity.publish();

        ActivityProfile profile = ProfileAdapter.toActivityProfile(activity, 2);

        assertThat(profile.id()).isEqualTo(40);
        assertThat(profile.location()).isEqualTo(new GeoPoint(50.08, 14.42));
        assertThat(profile.requiredSkills()).isEqualTo(Map.of(3L, 1.0));
        assertThat(profile.requiredCertificationIds()).containsExactly(6L);
        assertThat(profile.window()).isEqualTo(new TimeWindow(START, START.plusHours(2)));
        assertThat(profile.priority()).isEqualTo(Priority.URGENT);
        assertThat(profile.status()).isEqualTo(ActivityStatus.OPEN);
        assertThat(profile.freeSpots()).isEqualTo(3);
    }

    private static Activity activity(long id, int hours) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setLocation(GEOMETRY.createPoint(new Coordinate(14.42, 50.08)));
        activity.setStartsAt(START);
        activity.setEndsAt(START.plusHours(hours));
        return activity;
    }

    private static Skill skill(long id) {
        return Skill.builder().id(id).skillName("skill " + id).build();
    }

    private static Certification certification(long id) {
        return new Certification(id, "certification " + id);
    }
}
