package com.volunnear.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** The caller's own profile: the profile fields plus the username (Q3a). */
public record OwnVolunteerProfileResponseDto(
        String username,
        @JsonUnwrapped VolunteerProfileResponseDto profile
) {
}
