package com.volunnear.notification;

import com.volunnear.activity.Activity;
import com.volunnear.application.ActivityApplication;
import com.volunnear.application.ApplicationOrigin;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.application.ApplicationStatus;
import com.volunnear.auth.AppUser;
import com.volunnear.organization.Organization;
import com.volunnear.organization.OrganizationMemberRepository;
import com.volunnear.volunteer.Volunteer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Q7a … Q7c and Q8d: one notification per event, to the right address, with the texts built from the entities. */
class NotifierTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 7, 9, 0);

    private final List<Object> published = new ArrayList<>();
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final OrganizationMemberRepository memberRepository = mock(OrganizationMemberRepository.class);
    private final Notifier notifier = new Notifier(published::add, applicationRepository, memberRepository);

    private Activity activity;

    @BeforeEach
    void setUp() {
        Organization organization = new Organization();
        organization.setId(3L);
        organization.setOrganizationName("Food Bank");
        activity = new Activity(organization);
        activity.setId(5L);
        activity.setTitle("Sort donations");
        activity.setStartsAt(START);
        activity.setEndsAt(START.plusHours(3));
        when(memberRepository.findOwnerEmail(3L)).thenReturn(Optional.of("org@example.com"));
    }

    @Test
    void acceptedApplicationGoesToTheVolunteerWithTheOrganizationsContact() {
        notifier.applicationAccepted(application("anna@example.com"));

        assertThat(published).singleElement().isEqualTo(new Notification("anna@example.com", "Your application was accepted",
                "Food Bank accepted your application for \"Sort donations\" on 2026-11-07 09:00."
                        + " You can contact the organization at org@example.com."));
    }

    @Test
    void acceptedApplicationWithoutOwnerEmailStillGoesOutWithoutContact() {
        when(memberRepository.findOwnerEmail(3L)).thenReturn(Optional.empty());

        notifier.applicationAccepted(application("anna@example.com"));

        assertThat(only().body()).isEqualTo("Food Bank accepted your application for \"Sort donations\" on 2026-11-07 09:00.");
    }

    @Test
    void rejectedApplicationGoesToTheVolunteer() {
        notifier.applicationRejected(application("anna@example.com"));

        assertThat(only().to()).isEqualTo("anna@example.com");
        assertThat(only().subject()).isEqualTo("Your application was not accepted");
        assertThat(only().body()).isEqualTo("Food Bank did not accept your application for \"Sort donations\" on 2026-11-07 09:00.");
    }

    @Test
    void invitationGoesToTheVolunteer() {
        notifier.invitationReceived(application("anna@example.com"));

        assertThat(only().to()).isEqualTo("anna@example.com");
        assertThat(only().subject()).isEqualTo("You are invited to an activity");
        assertThat(only().body()).startsWith("Food Bank invites you to \"Sort donations\" on 2026-11-07 09:00.");
    }

    @Test
    void acceptedInvitationGoesToTheOrganizationOwner() {
        notifier.invitationAccepted(application("anna@example.com"));

        assertThat(only()).isEqualTo(new Notification("org@example.com", "Invitation accepted",
                "Anna Novak accepted your invitation to \"Sort donations\" on 2026-11-07 09:00."));
    }

    @Test
    void declinedInvitationGoesToTheOrganizationOwner() {
        notifier.invitationDeclined(application("anna@example.com"));

        assertThat(only()).isEqualTo(new Notification("org@example.com", "Invitation declined",
                "Anna Novak declined your invitation to \"Sort donations\" on 2026-11-07 09:00."));
    }

    @Test
    void withdrawalOfAnAcceptedVolunteerGoesToTheOrganizationOwner() {
        notifier.acceptedVolunteerWithdrew(application("anna@example.com"));

        assertThat(only()).isEqualTo(new Notification("org@example.com", "A volunteer withdrew",
                "Anna Novak withdrew from \"Sort donations\" on 2026-11-07 09:00. The spot is free again."));
    }

    @Test
    void cancellationGoesToEveryPendingOrAcceptedVolunteer() {
        when(applicationRepository.findWithVolunteerByActivityAndStatusIn(5L,
                List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED)))
                .thenReturn(List.of(application("anna@example.com"), application("ben@example.com")));

        notifier.activityCancelled(activity);

        assertThat(published).map(n -> ((Notification) n).to()).containsExactly("anna@example.com", "ben@example.com");
        assertThat(published).allSatisfy(n -> assertThat((Notification) n).isEqualTo(new Notification(((Notification) n).to(),
                "Activity cancelled", "Food Bank cancelled \"Sort donations\" on 2026-11-07 09:00.")));
    }

    @Test
    void completionGoesToEveryAcceptedVolunteerOnly() {
        // Q8d: the repository is asked for ACCEPTED rows only, so pending volunteers get nothing.
        when(applicationRepository.findWithVolunteerByActivityAndStatusIn(5L, List.of(ApplicationStatus.ACCEPTED)))
                .thenReturn(List.of(application("anna@example.com")));

        notifier.activityCompleted(activity);

        assertThat(only()).isEqualTo(new Notification("anna@example.com", "Activity completed",
                "Food Bank marked \"Sort donations\" on 2026-11-07 09:00 as completed. Thank you for taking part."));
    }

    @Test
    void missingVolunteerEmailSkipsTheNotification() {
        notifier.applicationAccepted(application(null));
        notifier.applicationRejected(application(" "));

        assertThat(published).isEmpty();
    }

    @Test
    void missingOwnerEmailSkipsTheNotification() {
        when(memberRepository.findOwnerEmail(3L)).thenReturn(Optional.empty());

        notifier.invitationDeclined(application("anna@example.com"));

        assertThat(published).isEmpty();
    }

    private Notification only() {
        assertThat(published).hasSize(1);
        return (Notification) published.get(0);
    }

    private ActivityApplication application(String email) {
        Volunteer volunteer = Volunteer.builder()
                .id(7L)
                .appUser(AppUser.builder().id(7L).username("anna").email(email).build())
                .firstName("Anna")
                .lastName("Novak")
                .build();
        return new ActivityApplication(volunteer, activity, ApplicationOrigin.APPLICATION, START.minusDays(1));
    }
}
