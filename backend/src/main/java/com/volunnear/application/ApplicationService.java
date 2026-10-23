package com.volunnear.application;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.application.dto.ActivityApplicationResponseDto;
import com.volunnear.application.dto.HistoryItemResponseDto;
import com.volunnear.application.dto.InvitationRequestDto;
import com.volunnear.application.dto.MyApplicationResponseDto;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.dictionary.Certification;
import com.volunnear.distribution.adapter.ProfileLoader;
import com.volunnear.distribution.feasibility.Feasibility;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.HeldCertification;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.notification.Notifier;
import com.volunnear.organization.OrganizationAccess;
import com.volunnear.volunteer.Volunteer;
import com.volunnear.volunteer.VolunteerMapper;
import com.volunnear.volunteer.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Applications (Q5a … Q5f) and invitations (Q5g … Q5m). Accept is the one place where spots and capacity are used up, so it locks
 * the activity row and then the volunteer row and re-checks everything under those locks (Q5d).
 * The repositories of activities and volunteers are used directly because accept needs their row locks.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;
    private final VolunteerRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;
    private final OrganizationAccess organizationAccess;
    private final ProfileLoader profileLoader;
    private final Notifier notifier;
    private final Clock clock;

    /** Q5c. Spots are checked here too, so a full activity answers at once; accept re-checks under the lock. */
    @Transactional
    public MyApplicationResponseDto apply(Long activityId, CustomUserDetails userDetails) {
        Volunteer volunteer = volunteerRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new DataNotFoundException("Volunteer profile of user " + userDetails.getUsername() + " not found"));
        Activity activity = activityRepository.findById(activityId)
                // A draft is invisible to volunteers (Q4e).
                .filter(found -> found.getStatus() != ActivityStatus.DRAFT)
                .orElseThrow(() -> activityNotFound(activityId));

        requireOpenAndNotStarted(activity);
        if (applicationRepository.existsByVolunteer_IdAndActivity_Id(volunteer.getId(), activityId)) {
            throw new ConflictException("You already applied to activity " + activityId);
        }
        requireFreeSpot(activity);
        requireCertifications(volunteer, activity);

        // The unique constraint still catches two concurrent first applications (answered with 409).
        ActivityApplication application = applicationRepository.saveAndFlush(
                new ActivityApplication(volunteer, activity, ApplicationOrigin.APPLICATION, now()));
        return toMyDto(application);
    }

    /** The organization accepts an application (Q5d). */
    @Transactional
    public ActivityApplicationResponseDto accept(Long applicationId, CustomUserDetails userDetails) {
        ActivityApplication application = find(applicationId);
        // The organization of an activity never changes, so it can be checked before the locks are taken.
        organizationAccess.requireManage(userDetails.getUser().getId(), application.getActivity().getOrganization().getId());
        application.requireOrigin(ApplicationOrigin.APPLICATION);
        acceptUnderLocks(application);
        notifier.applicationAccepted(application);
        return toActivityDto(application);
    }

    /**
     * Q5j: the organization invites a volunteer who passes the same filter as the recommendations, so an
     * invitation is never sent to someone the ranking would leave out. Each failed condition is named (409).
     */
    @Transactional
    public ActivityApplicationResponseDto invite(Long activityId, InvitationRequestDto request, CustomUserDetails userDetails) {
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> activityNotFound(activityId));
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        requireOpenAndNotStarted(activity);
        Volunteer volunteer = volunteerRepository.findById(request.volunteerId())
                .orElseThrow(() -> new DataNotFoundException("Volunteer " + request.volunteerId() + " not found"));

        VolunteerProfile volunteerProfile = profileLoader.volunteerProfile(volunteer);
        ActivityProfile activityProfile = profileLoader.activityProfile(activity);
        requireInvitable(volunteerProfile, activityProfile);

        // The unique constraint still catches an invitation racing the volunteer's own application (409).
        ActivityApplication invitation = applicationRepository.saveAndFlush(
                new ActivityApplication(volunteer, activity, ApplicationOrigin.INVITATION, now()));
        notifier.invitationReceived(invitation);
        return toActivityDto(invitation);
    }

    /** Q5k: the invited volunteer accepts, through the same locked path as an accepted application. */
    @Transactional
    public MyApplicationResponseDto acceptInvitation(Long applicationId, CustomUserDetails userDetails) {
        ActivityApplication invitation = find(applicationId);
        requireOwnApplication(invitation, userDetails, "Only the invited volunteer can accept an invitation");
        invitation.requireOrigin(ApplicationOrigin.INVITATION);
        acceptUnderLocks(invitation);
        notifier.invitationAccepted(invitation);
        return toMyDto(invitation);
    }

    /** Q5h. A pending invitation uses no spot or capacity, so declining needs no lock. */
    @Transactional
    public MyApplicationResponseDto declineInvitation(Long applicationId, CustomUserDetails userDetails) {
        ActivityApplication invitation = find(applicationId);
        requireOwnApplication(invitation, userDetails, "Only the invited volunteer can decline an invitation");
        invitation.decline(now());
        notifier.invitationDeclined(invitation);
        return toMyDto(invitation);
    }

    /**
     * Q5d, for applications and invitations alike. Lock order: activity, then volunteer.
     * Every check below reads committed data under these locks.
     */
    private void acceptUnderLocks(ActivityApplication application) {
        Long activityId = application.getActivity().getId();
        Long volunteerId = application.getVolunteer().getId();
        Activity activity = activityRepository.findByIdForUpdate(activityId).orElseThrow(() -> activityNotFound(activityId));
        Volunteer volunteer = volunteerRepository.findByIdForUpdate(volunteerId).orElseThrow();

        requireOpenAndNotStarted(activity);
        requireFreeSpot(activity);
        List<ActivityApplication> active = applicationRepository.findActiveAssignments(volunteerId);
        if (active.size() >= volunteer.getMaxActiveAssignments()) {
            throw new ConflictException("The volunteer has no free capacity (" + active.size() + " of "
                    + volunteer.getMaxActiveAssignments() + " active assignments)");
        }
        TimeWindow window = windowOf(activity);
        active.stream()
                .filter(other -> windowOf(other.getActivity()).overlaps(window))
                .findFirst()
                .ifPresent(other -> {
                    throw new ConflictException("The volunteer is already accepted for activity "
                            + other.getActivity().getId() + " at an overlapping time");
                });

        // The status check is the entity's; @Version makes a concurrent withdraw or decline fail.
        application.accept(now());
    }

    /**
     * The conditions of {@code Ranker} (PlatformEligibility and Feasibility) one by one, so the organization
     * learns which one failed. The predicates are the distribution's own; none is re-implemented here.
     */
    private static void requireInvitable(VolunteerProfile volunteer, ActivityProfile activity) {
        if (volunteer.banned()) {
            throw new ConflictException("Volunteer " + volunteer.id() + " cannot be invited");
        }
        if (volunteer.appliedActivityIds().contains(activity.id())) {
            throw new ConflictException("Volunteer " + volunteer.id() + " already has an application or invitation for activity "
                    + activity.id());
        }
        if (activity.freeSpots() <= 0) {
            throw new ConflictException("Activity " + activity.id() + " has no free spots");
        }
        if (!Feasibility.hasCapacity(volunteer)) {
            throw new ConflictException("Volunteer " + volunteer.id() + " has no free capacity");
        }
        if (!Feasibility.certified(volunteer, activity)) {
            throw new ConflictException("Volunteer " + volunteer.id() + " lacks a certification required by activity "
                    + activity.id());
        }
        if (!Feasibility.available(volunteer, activity)) {
            throw new ConflictException("Volunteer " + volunteer.id() + " is not available at the time of activity "
                    + activity.id());
        }
    }

    /**
     * Q25: a banned volunteer gives up everything on activities that have not started: pending applications are
     * withdrawn, pending invitations declined, accepted applications withdrawn with the organization notified.
     * The volunteer lock waits for a running accept; an accept that comes later fails on the application's @Version.
     */
    @Transactional
    public void releaseBannedVolunteer(Long volunteerId) {
        volunteerRepository.findByIdForUpdate(volunteerId)
                .orElseThrow(() -> new DataNotFoundException("Volunteer " + volunteerId + " not found"));
        for (ActivityApplication application : applicationRepository.findByVolunteerWithActivity(volunteerId)) {
            Activity activity = application.getActivity();
            if (activity.getStatus() != ActivityStatus.OPEN || !activity.getStartsAt().isAfter(now())) {
                continue;
            }
            switch (application.getStatus()) {
                case PENDING -> {
                    if (application.getOrigin() == ApplicationOrigin.INVITATION) {
                        application.decline(now());
                    } else {
                        application.withdraw(now());
                    }
                }
                case ACCEPTED -> {
                    application.withdraw(now());
                    notifier.acceptedVolunteerWithdrew(application);
                }
                default -> {
                    // Already decided; nothing to release.
                }
            }
        }
    }

    @Transactional
    public ActivityApplicationResponseDto reject(Long applicationId, CustomUserDetails userDetails) {
        ActivityApplication application = find(applicationId);
        organizationAccess.requireManage(userDetails.getUser().getId(), application.getActivity().getOrganization().getId());
        application.reject(now());
        notifier.applicationRejected(application);
        return toActivityDto(application);
    }

    /** Q5a: only the applicant; an accepted application only while the activity has not started. */
    @Transactional
    public MyApplicationResponseDto withdraw(Long applicationId, CustomUserDetails userDetails) {
        ActivityApplication application = find(applicationId);
        requireOwnApplication(application, userDetails, "Only the applicant can withdraw an application");
        boolean wasAccepted = application.getStatus() == ApplicationStatus.ACCEPTED;
        if (wasAccepted && !application.getActivity().getStartsAt().isAfter(now())) {
            throw new ConflictException("The activity has started; an accepted application can no longer be withdrawn");
        }
        application.withdraw(now());
        // Q7a: a pending application leaves silently; the organization sees it in its list.
        if (wasAccepted) {
            notifier.acceptedVolunteerWithdrew(application);
        }
        return toMyDto(application);
    }

    @Transactional(readOnly = true)
    public List<ActivityApplicationResponseDto> listForActivity(Long activityId, CustomUserDetails userDetails) {
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> activityNotFound(activityId));
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        return applicationRepository.findByActivity_IdOrderByAppliedAtAsc(activityId).stream()
                .map(this::toActivityDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MyApplicationResponseDto> listMine(CustomUserDetails userDetails) {
        return applicationRepository.findByVolunteerWithActivity(userDetails.getUser().getId()).stream()
                .map(this::toMyDto)
                .toList();
    }

    /** Q8e, Q8f: only the caller's own history; ownership follows from the session. */
    @Transactional(readOnly = true)
    public List<HistoryItemResponseDto> history(CustomUserDetails userDetails) {
        return applicationRepository.findHistory(userDetails.getUser().getId()).stream()
                .map(application -> {
                    Activity activity = application.getActivity();
                    return new HistoryItemResponseDto(application.getId(), activity.getId(), activity.getTitle(),
                            activity.getCity(), activity.getStartsAt(), activity.getEndsAt(),
                            activity.getOrganization().getId(), activity.getOrganization().getOrganizationName());
                })
                .toList();
    }

    private static void requireOwnApplication(ActivityApplication application, CustomUserDetails userDetails, String message) {
        if (!application.getVolunteer().getId().equals(userDetails.getUser().getId())) {
            throw new AccessDeniedException(message);
        }
    }

    private void requireOpenAndNotStarted(Activity activity) {
        if (activity.getStatus() != ActivityStatus.OPEN) {
            throw new ConflictException("Activity " + activity.getId() + " is " + activity.getStatus() + ", not OPEN");
        }
        if (!activity.getStartsAt().isAfter(now())) {
            throw new ConflictException("Activity " + activity.getId() + " has already started");
        }
    }

    private void requireFreeSpot(Activity activity) {
        if (activityRepository.countAccepted(activity.getId()) >= activity.getSpotsNeeded()) {
            throw new ConflictException("Activity " + activity.getId() + " has no free spots");
        }
    }

    // Q5c with Q14a: every required certification held and valid on the activity date.
    private static void requireCertifications(Volunteer volunteer, Activity activity) {
        LocalDate date = activity.getStartsAt().toLocalDate();
        List<String> missing = activity.getRequiredCertifications().stream()
                .filter(required -> volunteer.getCertifications().stream().noneMatch(held ->
                        held.getCertification().getId().equals(required.getId())
                                && new HeldCertification(required.getId(), held.getValidUntil()).isValidOn(date)))
                .map(Certification::getName)
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new ConflictException("Missing or expired certifications required by activity "
                    + activity.getId() + ": " + missing);
        }
    }

    private static TimeWindow windowOf(Activity activity) {
        return new TimeWindow(activity.getStartsAt(), activity.getEndsAt());
    }

    private ActivityApplication find(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new DataNotFoundException("Application " + applicationId + " not found"));
    }

    private static DataNotFoundException activityNotFound(Long activityId) {
        return new DataNotFoundException("Activity " + activityId + " not found");
    }

    private MyApplicationResponseDto toMyDto(ActivityApplication application) {
        Activity activity = application.getActivity();
        return new MyApplicationResponseDto(application.getId(), application.getStatus(), application.getOrigin(),
                application.getAppliedAt(), application.getDecidedAt(), activity.getId(), activity.getTitle(),
                activity.getStatus(), activity.getStartsAt(), activity.getEndsAt());
    }

    private ActivityApplicationResponseDto toActivityDto(ActivityApplication application) {
        // Q7d: the contact is shared only once the volunteer is accepted.
        String volunteerEmail = application.getStatus() == ApplicationStatus.ACCEPTED
                ? application.getVolunteer().getAppUser().getEmail()
                : null;
        return new ActivityApplicationResponseDto(application.getId(), application.getStatus(), application.getOrigin(),
                application.getAppliedAt(), application.getDecidedAt(), application.getActivity().getId(),
                volunteerMapper.toDto(application.getVolunteer()), volunteerEmail);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
