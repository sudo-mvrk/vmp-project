package com.volunnear.distribution;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityMapper;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.ActivityWeights;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.config.VolunnearProperties;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.distribution.adapter.ProfileAdapter;
import com.volunnear.distribution.adapter.ProfileLoader;
import com.volunnear.distribution.dto.RecommendedActivityDto;
import com.volunnear.distribution.dto.RecommendedVolunteerDto;
import com.volunnear.distribution.dto.ScoreDto;
import com.volunnear.distribution.dto.ScoringWeightsDto;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.ranking.Ranker;
import com.volunnear.distribution.scoring.ScoreBreakdown;
import com.volunnear.distribution.scoring.ScoringWeights;
import com.volunnear.organization.OrganizationAccess;
import com.volunnear.volunteer.Volunteer;
import com.volunnear.volunteer.VolunteerMapper;
import com.volunnear.volunteer.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Recommendations in both directions (Slice 6c). Loads the candidates, maps them with {@link ProfileAdapter}
 * (through {@link ProfileLoader}) and lets the pure {@link Ranker} filter, score and order them. No scoring or feasibility rule lives here.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ActivityRepository activityRepository;
    private final VolunteerRepository volunteerRepository;
    private final ProfileLoader profileLoader;
    private final OrganizationAccess organizationAccess;
    private final VolunteerMapper volunteerMapper;
    private final ActivityMapper activityMapper;
    private final VolunnearProperties properties;
    private final Clock clock;

    /** The best volunteers for an activity of the caller's organization, with its weight override if it has one (Q12). */
    @Transactional(readOnly = true)
    public List<RecommendedVolunteerDto> recommendVolunteers(Long activityId, int limit, CustomUserDetails userDetails) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DataNotFoundException("Activity " + activityId + " not found"));
        organizationAccess.requireManage(userDetails.getUser().getId(), activity.getOrganization().getId());
        // Q6a: the ranking would drop every candidate, so the caller is told why instead of getting an empty list.
        if (activity.getStatus() != ActivityStatus.OPEN) {
            throw new ConflictException("Activity " + activityId + " is " + activity.getStatus()
                    + "; recommendations need an OPEN activity");
        }
        if (!activity.getStartsAt().isAfter(now())) {
            throw new ConflictException("Activity " + activityId + " has already started");
        }

        ActivityProfile target = profileLoader.activityProfile(activity);
        Map<Long, Volunteer> candidates = byId(volunteerRepository.findRecommendationCandidates(), Volunteer::getId);
        ScoringWeights weights = weightsFor(activity);
        return Ranker.rankVolunteers(target, profileLoader.volunteerProfiles(candidates.values()), weights, limit).stream()
                .map(ranked -> new RecommendedVolunteerDto(
                        volunteerMapper.toDto(candidates.get(ranked.id())), toDto(ranked.score())))
                .toList();
    }

    /**
     * The best activities for the calling volunteer. Always the global weights (Q12), so an organization cannot
     * lift its activity in volunteers' lists. Activities outside the volunteer's radius are never loaded (Q6).
     */
    @Transactional(readOnly = true)
    public List<RecommendedActivityDto> recommendActivities(int limit, CustomUserDetails userDetails) {
        Volunteer volunteer = volunteerRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new DataNotFoundException(
                        "Volunteer profile of user " + userDetails.getUsername() + " not found"));
        Map<Long, Activity> candidates = byId(candidateActivities(volunteer), Activity::getId);

        return Ranker.rankActivities(profileLoader.volunteerProfile(volunteer),
                        profileLoader.activityProfiles(candidates.values()), properties.scoring().weights(), limit).stream()
                .map(ranked -> new RecommendedActivityDto(
                        activityMapper.toDto(candidates.get(ranked.id())), toDto(ranked.score())))
                .toList();
    }

    /**
     * Open activities that have not started (Q6c). With a location and a radius only those within the radius
     * (Q6, Q6d); without either, every one of them, ranked by u_geo (Q3g, Q5).
     */
    private List<Activity> candidateActivities(Volunteer volunteer) {
        if (volunteer.getLocation() == null || volunteer.getRadius() == null) {
            return activityRepository.findByStatusAndStartsAtAfter(ActivityStatus.OPEN, now());
        }
        List<Long> ids = activityRepository.findOpenIdsWithin(now(),
                volunteer.getLocation().getY(), volunteer.getLocation().getX(), volunteer.getRadius() * 1000.0);
        return activityRepository.findAllById(ids);
    }

    // Hibernate loads an override of three null columns as null; the service stores all three or none (Q12).
    private ScoringWeights weightsFor(Activity activity) {
        ActivityWeights override = activity.getWeights();
        if (override == null || override.alpha() == null) {
            return properties.scoring().weights();
        }
        return properties.scoring().weights(override.alpha(), override.beta(), override.gamma());
    }

    private ScoreDto toDto(ScoreBreakdown score) {
        ScoringWeights weights = score.weights();
        return new ScoreDto(score.geo(), score.skill(), score.priority(), score.total(),
                new ScoringWeightsDto(weights.alpha(), weights.beta(), weights.gamma(),
                        properties.scoring().halfDistanceKm()));
    }

    private static <T> Map<Long, T> byId(List<T> entities, Function<T, Long> id) {
        return entities.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
