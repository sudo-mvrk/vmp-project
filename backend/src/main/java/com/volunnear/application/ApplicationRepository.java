package com.volunnear.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<ActivityApplication, Long> {
    boolean existsByVolunteer_IdAndActivity_Id(Long volunteerId, Long activityId);

    /**
     * The volunteer's active assignments (Q16): accepted applications on activities that are
     * neither completed nor cancelled. Their number is the used capacity.
     */
    @Query("""
            select a from ActivityApplication a join fetch a.activity t
            where a.volunteer.id = :volunteerId
              and a.status = com.volunnear.application.ApplicationStatus.ACCEPTED
              and t.status not in (com.volunnear.activity.ActivityStatus.COMPLETED,
                                   com.volunnear.activity.ActivityStatus.CANCELLED)
            """)
    List<ActivityApplication> findActiveAssignments(@Param("volunteerId") Long volunteerId);

    /** {@link #findActiveAssignments} for many volunteers at once (recommendations, Slice 6c). */
    @Query("""
            select a from ActivityApplication a join fetch a.activity t
            where a.volunteer.id in :volunteerIds
              and a.status = com.volunnear.application.ApplicationStatus.ACCEPTED
              and t.status not in (com.volunnear.activity.ActivityStatus.COMPLETED,
                                   com.volunnear.activity.ActivityStatus.CANCELLED)
            """)
    List<ActivityApplication> findActiveAssignmentsOfVolunteers(@Param("volunteerIds") Collection<Long> volunteerIds);

    /** Every activity the volunteers have an application for, in any status (Q5b, Q15). */
    @Query("""
            select a.volunteer.id as volunteerId, a.activity.id as activityId from ActivityApplication a
            where a.volunteer.id in :volunteerIds
            """)
    List<AppliedActivity> findAppliedActivities(@Param("volunteerIds") Collection<Long> volunteerIds);

    interface AppliedActivity {
        Long getVolunteerId();

        Long getActivityId();
    }

    List<ActivityApplication> findByActivity_IdOrderByAppliedAtAsc(Long activityId);

    /** The applications of an activity in the given statuses, with the volunteer's account (Q7b). */
    @Query("""
            select a from ActivityApplication a join fetch a.volunteer v join fetch v.appUser
            where a.activity.id = :activityId and a.status in :statuses
            """)
    List<ActivityApplication> findWithVolunteerByActivityAndStatusIn(@Param("activityId") Long activityId,
                                                                    @Param("statuses") Collection<ApplicationStatus> statuses);

    /** Q8e: the volunteer's accepted applications on completed activities, newest activity first. */
    @Query("""
            select a from ActivityApplication a join fetch a.activity t join fetch t.organization
            where a.volunteer.id = :volunteerId
              and a.status = com.volunnear.application.ApplicationStatus.ACCEPTED
              and t.status = com.volunnear.activity.ActivityStatus.COMPLETED
            order by t.startsAt desc, t.id desc
            """)
    List<ActivityApplication> findHistory(@Param("volunteerId") Long volunteerId);

    @Query("select a from ActivityApplication a join fetch a.activity where a.volunteer.id = :volunteerId order by a.appliedAt desc")
    List<ActivityApplication> findByVolunteerWithActivity(@Param("volunteerId") Long volunteerId);
}
