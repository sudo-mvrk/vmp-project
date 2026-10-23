package com.volunnear.application;

import com.volunnear.activity.Activity;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.volunteer.Volunteer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * An application of a volunteer to an activity, or an invitation of the organization (Q5g). The transition
 * table of Q5a, Q5h and Q5i lives here, including which origin each decision belongs to;
 * the service checks who may act and the activity and capacity conditions (Q5c, Q5d, Q5j).
 */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "applications")
public class ActivityApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volunteer_id", nullable = false, updatable = false)
    private Volunteer volunteer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, updatable = false)
    private Activity activity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ApplicationOrigin origin;
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;
    private LocalDateTime decidedAt;
    @Version
    private long version;

    public ActivityApplication(Volunteer volunteer, Activity activity, ApplicationOrigin origin, LocalDateTime now) {
        this.volunteer = volunteer;
        this.activity = activity;
        this.origin = origin;
        this.status = ApplicationStatus.PENDING;
        this.appliedAt = now;
    }

    public void accept(LocalDateTime now) {
        requireStatus(ApplicationStatus.PENDING, "accepted");
        status = ApplicationStatus.ACCEPTED;
        decidedAt = now;
    }

    /** The organization turns down an application; an invitation is declined by the volunteer instead. */
    public void reject(LocalDateTime now) {
        requireOrigin(ApplicationOrigin.APPLICATION);
        requireStatus(ApplicationStatus.PENDING, "rejected");
        status = ApplicationStatus.REJECTED;
        decidedAt = now;
    }

    /** Q5h: the volunteer turns down an invitation. */
    public void decline(LocalDateTime now) {
        requireOrigin(ApplicationOrigin.INVITATION);
        requireStatus(ApplicationStatus.PENDING, "declined");
        status = ApplicationStatus.DECLINED;
        decidedAt = now;
    }

    /**
     * From ACCEPTED, or from PENDING for an application; a pending invitation is declined instead (Q5i).
     * The service checks that an accepted activity has not started.
     */
    public void withdraw(LocalDateTime now) {
        if (status == ApplicationStatus.PENDING && origin == ApplicationOrigin.INVITATION) {
            throw new ConflictException("Invitation " + id + " is pending; decline it instead of withdrawing");
        }
        if (status != ApplicationStatus.PENDING && status != ApplicationStatus.ACCEPTED) {
            throw new ConflictException("Application " + id + " is " + status + " and cannot be withdrawn");
        }
        status = ApplicationStatus.WITHDRAWN;
        decidedAt = now;
    }

    /** Q5i: an application is decided by the organization, an invitation by the volunteer. */
    public void requireOrigin(ApplicationOrigin required) {
        if (origin != required) {
            throw new ConflictException(required == ApplicationOrigin.INVITATION
                    ? "Application " + id + " is not an invitation; the organization decides it"
                    : "Application " + id + " is an invitation; the volunteer decides it");
        }
    }

    private void requireStatus(ApplicationStatus required, String action) {
        if (status != required) {
            throw new ConflictException("Only a " + required + " application can be " + action
                    + "; application " + id + " is " + status);
        }
    }
}
