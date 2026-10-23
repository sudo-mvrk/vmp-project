package com.volunnear.activity.dto;

import com.volunnear.activity.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Create, or replace as a whole (Q4c). Times are local times in the configured zone (Q4a);
 * the service checks the window (same day, start in the future).
 *
 * @param priority  absent means MEDIUM (Q8)
 * @param weights   absent means the global weights apply (Q12)
 */
@Builder
public record ActivityRequestDto(
        @NotBlank @Size(max = 150)
        String title,
        @Size(max = 500)
        String shortDescription,
        String fullDescription,
        Priority priority,
        @NotNull
        @DecimalMin(value = "-90.0", message = "Latitude can't be less than -90")
        @DecimalMax(value = "90.0", message = "Latitude can't be more than 90")
        Double lat,
        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude can't be less than -180")
        @DecimalMax(value = "180.0", message = "Longitude can't be more than 180")
        Double lon,
        @Size(max = 255)
        String city,
        @Size(max = 255)
        String fullAddress,
        @NotNull
        LocalDateTime startsAt,
        @NotNull
        LocalDateTime endsAt,
        @NotNull
        @Min(value = 1, message = "An activity needs at least 1 spot")
        @Max(value = 1000, message = "An activity has at most 1000 spots")
        Integer spotsNeeded,
        List<@NotNull Long> requiredSkillIds,
        List<@NotNull Long> requiredCertificationIds,
        @Valid
        ActivityWeightsDto weights
) {
}
