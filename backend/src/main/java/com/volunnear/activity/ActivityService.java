package com.volunnear.activity;

import com.volunnear.activity.dto.ActivityPageResponseDto;
import com.volunnear.activity.dto.ActivityRequestDto;
import com.volunnear.activity.dto.ActivityResponseDto;
import com.volunnear.activity.dto.ActivityWeightsDto;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.DictionaryService;
import com.volunnear.dictionary.Skill;
import com.volunnear.distribution.scoring.ScoringWeights;
import com.volunnear.notification.Notifier;
import com.volunnear.organization.OrganizationAccess;
import com.volunnear.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activities of organizations. Every change is checked with {@link OrganizationAccess#requireManage}
 * against the activity's organization (403); the status rules live on {@link Activity} (409).
 */
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final OrganizationService organizationService;
    private final OrganizationAccess organizationAccess;
    private final DictionaryService dictionaryService;
    private final Notifier notifier;
    private final Clock clock;

    @Transactional
    public ActivityResponseDto create(ActivityRequestDto request, CustomUserDetails userDetails) {
        // The new activity belongs to the caller's own organization, so no further ownership check is needed.
        Activity activity = new Activity(organizationService.getMyOrganizationEntity(userDetails));
        apply(request, activity);
        return activityMapper.toDto(activityRepository.save(activity), 0);
    }

    @Transactional
    public ActivityResponseDto update(Long activityId, ActivityRequestDto request, CustomUserDetails userDetails) {
        // Locked like an accept, so the accepted count cannot change between this check and the write.
        Activity activity = activityRepository.findByIdForUpdate(activityId).orElseThrow(() -> notFound(activityId));
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        activity.requireEditable();
        long accepted = activityRepository.countAccepted(activityId);
        if (request.spotsNeeded() < accepted) {
            throw new ConflictException("Activity " + activityId + " already has " + accepted
                    + " accepted volunteers; spotsNeeded cannot be lower");
        }
        apply(request, activity);
        return activityMapper.toDto(activity, accepted);
    }

    @Transactional
    public ActivityResponseDto publish(Long activityId, CustomUserDetails userDetails) {
        Activity activity = findManaged(activityId, userDetails);
        // A draft may have been created long ago; it cannot open after its start (Q4d).
        // Only checked for drafts, so publishing anything else is still answered with 409 by publish().
        if (activity.getStatus() == ActivityStatus.DRAFT) {
            ActivityWindowRules.requireFutureStart(activity.getStartsAt(), now());
        }
        activity.publish();
        return toDto(activity);
    }

    @Transactional
    public ActivityResponseDto cancel(Long activityId, CustomUserDetails userDetails) {
        return cancelLocked(findManagedForUpdate(activityId, userDetails));
    }

    /** Q31: an admin removes an activity by cancelling it. The caller ({@code AdminService}) checks the admin role. */
    @Transactional
    public ActivityResponseDto cancelAsAdmin(Long activityId) {
        return cancelLocked(findForUpdate(activityId));
    }

    /**
     * Q26: the activities of a banned organization that have not started are cancelled, with the usual notifications.
     * Only ids are read first, so each activity is loaded fresh under its lock.
     */
    @Transactional
    public void cancelUpcomingOf(Long organizationId) {
        for (Long activityId : activityRepository.findUpcomingIdsOfOrganization(organizationId, now())) {
            Activity activity = findForUpdate(activityId);
            if (activity.getStatus() == ActivityStatus.DRAFT || activity.getStatus() == ActivityStatus.OPEN) {
                cancelLocked(activity);
            }
        }
    }

    private ActivityResponseDto cancelLocked(Activity activity) {
        activity.cancel();
        notifier.activityCancelled(activity);
        return toDto(activity);
    }

    /** Q8a, Q8d. Capacity needs no update: it counts only activities that are not completed (Q16). */
    @Transactional
    public ActivityResponseDto complete(Long activityId, CustomUserDetails userDetails) {
        Activity activity = findManagedForUpdate(activityId, userDetails);
        if (activity.getStatus() == ActivityStatus.OPEN && activity.getStartsAt().isAfter(now())) {
            throw new ConflictException("Activity " + activityId + " has not started yet and cannot be completed");
        }
        activity.complete();
        notifier.activityCompleted(activity);
        return toDto(activity);
    }

    /** Public read (Q4e). A draft is visible only to its organization; to anyone else it does not exist. */
    @Transactional(readOnly = true)
    public ActivityResponseDto get(Long activityId, CustomUserDetails userDetailsOrNull) {
        Activity activity = find(activityId);
        if (activity.getStatus() == ActivityStatus.DRAFT
                && (userDetailsOrNull == null
                || !organizationAccess.canManage(userDetailsOrNull.getUser().getId(), activity.getOrganization().getId()))) {
            throw notFound(activityId);
        }
        return toDto(activity);
    }

    /** Public list (Q4e): open activities that have not started yet, soonest first. */
    @Transactional(readOnly = true)
    public ActivityPageResponseDto listOpen(int page, int size) {
        Page<Activity> result = activityRepository.findByStatusAndStartsAtAfter(ActivityStatus.OPEN, now(),
                PageRequest.of(page, size, Sort.by("startsAt", "id")));
        return new ActivityPageResponseDto(toDtos(result.getContent()),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ActivityResponseDto> listMine(CustomUserDetails userDetails) {
        Long organizationId = organizationService.getMyOrganizationEntity(userDetails).getId();
        return toDtos(activityRepository.findByOrganization_IdOrderByStartsAtAsc(organizationId));
    }

    private ActivityResponseDto toDto(Activity activity) {
        return activityMapper.toDto(activity, activityRepository.countAccepted(activity.getId()));
    }

    /** One query for the accepted counts of the whole list. */
    private List<ActivityResponseDto> toDtos(List<Activity> activities) {
        Map<Long, Long> accepted = activityRepository.acceptedCounts(activities.stream().map(Activity::getId).toList());
        return activities.stream()
                .map(activity -> activityMapper.toDto(activity, accepted.getOrDefault(activity.getId(), 0L)))
                .toList();
    }

    /** Validates the whole request first, then writes it, so a rejected request changes nothing. */
    private void apply(ActivityRequestDto request, Activity activity) {
        // Activities are planned to the minute, like availability slots.
        LocalDateTime startsAt = request.startsAt().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime endsAt = request.endsAt().truncatedTo(ChronoUnit.MINUTES);
        ActivityWindowRules.validate(startsAt, endsAt, now());
        ActivityWeights weights = toWeights(request.weights());
        Set<Skill> skills = dictionaryService.requireSkills(idsOf(request.requiredSkillIds()));
        Map<Long, Certification> certifications = dictionaryService.requireCertifications(idsOf(request.requiredCertificationIds()));

        activity.setTitle(request.title());
        activity.setShortDescription(request.shortDescription());
        activity.setFullDescription(request.fullDescription());
        activity.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        activity.setLocation(ActivityMapper.GEOMETRY_FACTORY.createPoint(new Coordinate(request.lon(), request.lat())));
        activity.setCity(request.city());
        activity.setFullAddress(request.fullAddress());
        activity.setStartsAt(startsAt);
        activity.setEndsAt(endsAt);
        activity.setSpotsNeeded(request.spotsNeeded());
        activity.setWeights(weights);
        activity.getRequiredSkills().clear();
        activity.getRequiredSkills().addAll(skills);
        activity.getRequiredCertifications().clear();
        activity.getRequiredCertifications().addAll(certifications.values());
    }

    // Q12: the same rule as the global weights, so an override can never break the scoring.
    private static ActivityWeights toWeights(ActivityWeightsDto dto) {
        if (dto == null) {
            return null;
        }
        try {
            ScoringWeights.requireOnSimplex(dto.alpha(), dto.beta(), dto.gamma());
        } catch (IllegalArgumentException e) {
            throw new BadDataInRequestException("Invalid weights: " + e.getMessage());
        }
        return new ActivityWeights(dto.alpha(), dto.beta(), dto.gamma());
    }

    private static Set<Long> idsOf(List<Long> ids) {
        return ids == null ? Set.of() : new HashSet<>(ids);
    }

    private Activity findManaged(Long activityId, CustomUserDetails userDetails) {
        Activity activity = find(activityId);
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        return activity;
    }

    // Cancel and complete both end an open activity; the row lock stops them from overwriting each other.
    private Activity findManagedForUpdate(Long activityId, CustomUserDetails userDetails) {
        Activity activity = findForUpdate(activityId);
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        return activity;
    }

    private Activity findForUpdate(Long activityId) {
        return activityRepository.findByIdForUpdate(activityId).orElseThrow(() -> notFound(activityId));
    }

    private Activity find(Long activityId) {
        return activityRepository.findById(activityId).orElseThrow(() -> notFound(activityId));
    }

    private static DataNotFoundException notFound(Long activityId) {
        return new DataNotFoundException("Activity " + activityId + " not found");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
