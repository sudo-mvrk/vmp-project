package com.volunnear.dictionary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "skill_name", nullable = false, unique = true)
    private String skillName;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;
}
