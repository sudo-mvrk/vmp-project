package com.volunnear.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Per-activity weight override (Q12). Used only when ranking volunteers for this activity.
 * Hibernate stores a null override as three null columns.
 */
@Embeddable
public record ActivityWeights(
        @Column(name = "weight_alpha") Double alpha,
        @Column(name = "weight_beta") Double beta,
        @Column(name = "weight_gamma") Double gamma
) {
}
