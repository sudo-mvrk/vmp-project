package com.volunnear.activity;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    // Q4e: the public list shows open activities that have not started yet.
    Page<Activity> findByStatusAndStartsAtAfter(ActivityStatus status, LocalDateTime now, Pageable pageable);

    // Candidates of volunteer -> activities when the volunteer has no radius (Q3g): every open activity not started yet.
    List<Activity> findByStatusAndStartsAtAfter(ActivityStatus status, LocalDateTime now);

    /**
     * Radius prefilter of volunteer -> activities (Q6, Q6c, Q6d): ids of open activities that have not started
     * and lie within {@code meters} of the point. use_spheroid = false measures on a sphere like the haversine
     * distance of the scoring, so both agree at the edge; the edge itself counts as inside.
     */
    @Query(value = """
            select a.id from activities a
            where a.status = 'OPEN' and a.starts_at > :now
              and ST_DWithin(a.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :meters, false)
            """, nativeQuery = true)
    List<Long> findOpenIdsWithin(@Param("now") LocalDateTime now, @Param("lat") double lat, @Param("lon") double lon,
                                 @Param("meters") double meters);

    List<Activity> findByOrganization_IdOrderByStartsAtAsc(Long organizationId);

    /** Q26: draft or open activities of the organization that have not started; ids only, so they are locked fresh. */
    @Query("""
            select a.id from Activity a
            where a.organization.id = :organizationId and a.startsAt > :now
              and a.status in (com.volunnear.activity.ActivityStatus.DRAFT, com.volunnear.activity.ActivityStatus.OPEN)
            order by a.id
            """)
    List<Long> findUpcomingIdsOfOrganization(@Param("organizationId") Long organizationId, @Param("now") LocalDateTime now);

    /**
     * SELECT ... FOR UPDATE: serialises everything that changes the spots of one activity
     * (accept, update of spotsNeeded). Lock order is always activity, then volunteer (Q5d).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a where a.id = :id")
    Optional<Activity> findByIdForUpdate(@Param("id") Long id);

    // Free spots = spotsNeeded minus this count. A query, so it always reads committed rows, not the session cache.
    @Query("""
            select count(a) from ActivityApplication a
            where a.activity.id = :activityId and a.status = com.volunnear.application.ApplicationStatus.ACCEPTED
            """)
    long countAccepted(@Param("activityId") Long activityId);

    /** {@link #countAccepted} for many activities at once; activities without an accepted volunteer are absent. */
    @Query("""
            select a.activity.id as activityId, count(a) as accepted from ActivityApplication a
            where a.activity.id in :activityIds and a.status = com.volunnear.application.ApplicationStatus.ACCEPTED
            group by a.activity.id
            """)
    List<AcceptedCount> countAcceptedByActivity(@Param("activityIds") Collection<Long> activityIds);

    /** {@link #countAcceptedByActivity} as a map; activities without an accepted volunteer are absent (count 0). */
    default Map<Long, Long> acceptedCounts(Collection<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return countAcceptedByActivity(activityIds).stream()
                .collect(Collectors.toMap(AcceptedCount::getActivityId, AcceptedCount::getAccepted));
    }

    interface AcceptedCount {
        Long getActivityId();

        long getAccepted();
    }
}
