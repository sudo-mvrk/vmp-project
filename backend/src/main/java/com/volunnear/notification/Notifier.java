package com.volunnear.notification;

import com.volunnear.activity.Activity;
import com.volunnear.application.ActivityApplication;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.application.ApplicationStatus;
import com.volunnear.organization.OrganizationMemberRepository;
import com.volunnear.volunteer.Volunteer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * The notification events of Q7a, Q7b and Q8d. Called by the services inside their transaction: it reads the
 * recipient and builds the text while the entities are still loaded, then publishes a {@link Notification}
 * that {@link NotificationDispatcher} sends after the commit (Q7f).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Notifier {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ApplicationEventPublisher publisher;
    private final ApplicationRepository applicationRepository;
    private final OrganizationMemberRepository memberRepository;

    /** Q7d: the volunteer learns how to reach the organization. */
    public void applicationAccepted(ActivityApplication application) {
        Activity activity = application.getActivity();
        String contact = ownerEmail(activity)
                .map(email -> " You can contact the organization at " + email + ".")
                .orElse("");
        toVolunteer(application.getVolunteer(), "Your application was accepted",
                organizationName(activity) + " accepted your application for " + describe(activity) + "." + contact);
    }

    public void applicationRejected(ActivityApplication application) {
        Activity activity = application.getActivity();
        toVolunteer(application.getVolunteer(), "Your application was not accepted",
                organizationName(activity) + " did not accept your application for " + describe(activity) + ".");
    }

    public void invitationReceived(ActivityApplication invitation) {
        Activity activity = invitation.getActivity();
        toVolunteer(invitation.getVolunteer(), "You are invited to an activity",
                organizationName(activity) + " invites you to " + describe(activity) + ". Accept or decline the invitation "
                        + invitation.getId() + " in your applications.");
    }

    public void invitationAccepted(ActivityApplication invitation) {
        toOrganization(invitation.getActivity(), "Invitation accepted",
                nameOf(invitation.getVolunteer()) + " accepted your invitation to " + describe(invitation.getActivity()) + ".");
    }

    public void invitationDeclined(ActivityApplication invitation) {
        toOrganization(invitation.getActivity(), "Invitation declined",
                nameOf(invitation.getVolunteer()) + " declined your invitation to " + describe(invitation.getActivity()) + ".");
    }

    public void acceptedVolunteerWithdrew(ActivityApplication application) {
        toOrganization(application.getActivity(), "A volunteer withdrew",
                nameOf(application.getVolunteer()) + " withdrew from " + describe(application.getActivity())
                        + ". The spot is free again.");
    }

    /** Q7b: every volunteer with a pending or accepted application or invitation, one notification each. */
    public void activityCancelled(Activity activity) {
        List<ActivityApplication> affected = applicationRepository.findWithVolunteerByActivityAndStatusIn(
                activity.getId(), List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED));
        for (ActivityApplication application : affected) {
            toVolunteer(application.getVolunteer(), "Activity cancelled",
                    organizationName(activity) + " cancelled " + describe(activity) + ".");
        }
    }

    /** Q8d: every accepted volunteer of a completed activity, one notification each. */
    public void activityCompleted(Activity activity) {
        List<ActivityApplication> accepted = applicationRepository.findWithVolunteerByActivityAndStatusIn(
                activity.getId(), List.of(ApplicationStatus.ACCEPTED));
        for (ActivityApplication application : accepted) {
            toVolunteer(application.getVolunteer(), "Activity completed",
                    organizationName(activity) + " marked " + describe(activity)
                            + " as completed. Thank you for taking part.");
        }
    }

    private void toVolunteer(Volunteer volunteer, String subject, String body) {
        publish(volunteer.getAppUser().getEmail(), "volunteer " + volunteer.getId(), subject, body);
    }

    private void toOrganization(Activity activity, String subject, String body) {
        publish(ownerEmail(activity).orElse(null), "organization " + activity.getOrganization().getId(), subject, body);
    }

    // Q7c: the email column allows null; a missing address skips the notification, it is not an error.
    private void publish(String to, String recipient, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("No email for {}; notification \"{}\" skipped", recipient, subject);
            return;
        }
        publisher.publishEvent(new Notification(to, subject, body));
    }

    private Optional<String> ownerEmail(Activity activity) {
        return memberRepository.findOwnerEmail(activity.getOrganization().getId());
    }

    private static String organizationName(Activity activity) {
        return activity.getOrganization().getOrganizationName();
    }

    private static String describe(Activity activity) {
        return "\"" + activity.getTitle() + "\" on " + WHEN.format(activity.getStartsAt());
    }

    private static String nameOf(Volunteer volunteer) {
        return volunteer.getFirstName() + " " + volunteer.getLastName();
    }
}
