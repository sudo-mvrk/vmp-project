package com.volunnear.seed;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Sizes of the seed data (Q37), only read under the {@code seed} profile. The fixed random seed makes the data
 * the same on every load, so the evaluation is reproducible. The minimums keep the demo states of Q35 possible.
 */
@Validated
@ConfigurationProperties(prefix = "volunnear.seed")
public record SeedProperties(
        @DefaultValue("3") @Min(1) @Max(50) int organizations,
        @DefaultValue("30") @Min(12) @Max(5000) int volunteers,
        @DefaultValue("10") @Min(2) @Max(500) int activities,
        @DefaultValue("42") long randomSeed
) {
}
