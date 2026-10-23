package com.volunnear.activity;

import com.volunnear.common.exception.ConflictException;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.Skill;
import com.volunnear.organization.Organization;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * An activity of an organization. Status changes go through {@link #publish()} and {@link #cancel()},
 * which enforce the transitions of Q4d; the service only decides who may call them.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(length = 500)
    private String shortDescription;
    @Column(columnDefinition = "text")
    private String fullDescription;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.DRAFT;
    @Column(nullable = false)
    private Point location;
    private String city;
    private String fullAddress;
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private LocalDateTime endsAt;
    @Column(nullable = false)
    private int spotsNeeded;
    @Embedded
    private ActivityWeights weights;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Batch fetching keeps list pages at a few queries instead of one per activity.
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "activity_required_skill",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> requiredSkills = new HashSet<>();

    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "activity_required_certification",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "certification_id"))
    private Set<Certification> requiredCertifications = new HashSet<>();

    public Activity(Organization organization) {
        this.organization = organization;
    }

    /** Q4c: only a draft or an open activity can be edited. */
    public void requireEditable() {
        if (status != ActivityStatus.DRAFT && status != ActivityStatus.OPEN) {
            throw new ConflictException("Activity " + id + " is " + status + " and can no longer be edited");
        }
    }

    /** Q4d: DRAFT → OPEN. The service checks the start time before calling it. */
    public void publish() {
        if (status != ActivityStatus.DRAFT) {
            throw new ConflictException("Only a DRAFT activity can be published; activity " + id + " is " + status);
        }
        status = ActivityStatus.OPEN;
    }

    /** Q4d: DRAFT or OPEN → CANCELLED. */
    public void cancel() {
        if (status != ActivityStatus.DRAFT && status != ActivityStatus.OPEN) {
            throw new ConflictException("Only a DRAFT or OPEN activity can be cancelled; activity " + id + " is " + status);
        }
        status = ActivityStatus.CANCELLED;
    }

    /** Q8a: OPEN → COMPLETED. The service checks that the activity has started. */
    public void complete() {
        if (status != ActivityStatus.OPEN) {
            throw new ConflictException("Only an OPEN activity can be completed; activity " + id + " is " + status);
        }
        status = ActivityStatus.COMPLETED;
    }
}
