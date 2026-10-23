package com.volunnear.volunteer;

import com.volunnear.dictionary.Certification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** A certification the volunteer declares (Q14). validUntil is inclusive; null means it does not expire. */
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerCertification {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;
    @Column(name = "valid_until")
    private LocalDate validUntil;
}
