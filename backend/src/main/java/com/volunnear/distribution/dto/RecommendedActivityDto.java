package com.volunnear.distribution.dto;

import com.volunnear.activity.dto.ActivityResponseDto;

/** One entry of volunteer -> activities. */
public record RecommendedActivityDto(ActivityResponseDto activity, ScoreDto score) {
}
