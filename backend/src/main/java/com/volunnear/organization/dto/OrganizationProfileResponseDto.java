package com.volunnear.organization.dto;

// Public view of an organization (Q2a): no username or email of its accounts.
public record OrganizationProfileResponseDto(
        Long id,
        String organizationName,
        String description,
        String websiteUrl,
        Double lat,
        Double lon,
        String city,
        String fullAddress,
        String countryCode
) {
}
