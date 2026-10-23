package com.volunnear.organization.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** The caller's own organization: the public fields plus the caller's username (Q3a, Q3b). */
public record OwnOrganizationProfileResponseDto(
        String username,
        @JsonUnwrapped OrganizationProfileResponseDto profile
) {
}
