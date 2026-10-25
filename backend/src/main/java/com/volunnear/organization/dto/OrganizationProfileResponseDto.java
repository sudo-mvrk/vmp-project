package com.volunnear.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Public view of an organization (Q2a): no username or email of its accounts.
public record OrganizationProfileResponseDto(
        Long id,
        String organizationName,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String websiteUrl,
        @Schema(nullable = true) Double lat,
        @Schema(nullable = true) Double lon,
        @Schema(nullable = true) String city,
        @Schema(nullable = true) String fullAddress,
        @Schema(nullable = true) String countryCode
) {
}
