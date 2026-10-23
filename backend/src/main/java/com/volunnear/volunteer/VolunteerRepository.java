package com.volunnear.volunteer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// A volunteer shares its id with the user account (@MapsId), so findById(userId) finds the caller's profile.
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    /** SELECT ... FOR UPDATE: serialises accepts that use this volunteer's capacity (Q5d). Lock after the activity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Volunteer v where v.id = :id")
    Optional<Volunteer> findByIdForUpdate(@Param("id") Long id);

    /**
     * Candidates of activity -> volunteers (Q6f): banned volunteers and those taking no assignments (Q3e) can never
     * be feasible, so they are skipped here; every other rule is checked by the pure ranking code.
     */
    @Query("select v from Volunteer v join v.appUser u where u.isBanned = false and v.maxActiveAssignments > 0")
    List<Volunteer> findRecommendationCandidates();
}
