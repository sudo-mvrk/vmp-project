package com.volunnear.dictionary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Certification dictionary entry (Q14). Rows are seeded by migration; there is no write endpoint. */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "certifications")
public class Certification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
}
