package com.volunnear.volunteer;

import com.volunnear.auth.AppUser;
import com.volunnear.dictionary.Skill;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "volunteers")
public class Volunteer {
    @Id
    private Long id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser appUser;
    private String firstName;
    private String lastName;
    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;
    @Column(columnDefinition = "text")
    private String bio;
    private Point location;
    private String locationName;
    @Column(length = 2)
    private String countryCode;
    private String city;
    private String region;
    // Kilometres; used only by the radius prefilter of Slice 6c (Q6, Q3g).
    private Integer radius;
    // Q16, Q3e: how many accepted, not yet finished activities the volunteer takes at once.
    @Builder.Default
    @Column(nullable = false)
    private int maxActiveAssignments = 1;

    // Batch fetching keeps the recommendation candidates at a few queries instead of three per volunteer.
    @Builder.Default
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "volunteer_skill",
            joinColumns = @JoinColumn(name = "volunteer_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    @Builder.Default
    @BatchSize(size = 50)
    @ElementCollection
    @CollectionTable(name = "volunteer_certification", joinColumns = @JoinColumn(name = "volunteer_id"))
    private List<VolunteerCertification> certifications = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 50)
    @ElementCollection
    @CollectionTable(name = "volunteer_availability", joinColumns = @JoinColumn(name = "volunteer_id"))
    private List<VolunteerAvailability> availability = new ArrayList<>();
}
