package com.volunnear.distribution;

import com.volunnear.auth.CustomUserDetails;
import com.volunnear.distribution.dto.RecommendedActivityDto;
import com.volunnear.distribution.dto.RecommendedVolunteerDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ranked top-N lists with the score breakdown (Q17). {@code limit} is 1 ... 50, default 10 (Q6b). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/activities/{id}/recommended-volunteers")
    public ResponseEntity<List<RecommendedVolunteerDto>> recommendVolunteers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recommendationService.recommendVolunteers(id, limit, userDetails));
    }

    @GetMapping("/volunteers/me/recommended-activities")
    public ResponseEntity<List<RecommendedActivityDto>> recommendActivities(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recommendationService.recommendActivities(limit, userDetails));
    }
}
