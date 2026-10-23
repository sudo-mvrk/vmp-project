package com.volunnear.distribution.dto;

import com.volunnear.volunteer.dto.VolunteerProfileResponseDto;

/** One entry of activity -> volunteers. The profile has no account data (Q3a). */
public record RecommendedVolunteerDto(VolunteerProfileResponseDto volunteer, ScoreDto score) {
}
