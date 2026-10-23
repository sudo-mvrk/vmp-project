package com.volunnear.activity;

import com.volunnear.activity.dto.ActivityPageResponseDto;
import com.volunnear.activity.dto.ActivityRequestDto;
import com.volunnear.activity.dto.ActivityResponseDto;
import com.volunnear.auth.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ActivityController {
    private final ActivityService activityService;

    @PostMapping("/activities")
    public ResponseEntity<ActivityResponseDto> create(@RequestBody @Valid ActivityRequestDto requestDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(requestDto, userDetails));
    }

    @PutMapping("/activities/{id}")
    public ResponseEntity<ActivityResponseDto> update(@PathVariable Long id,
                                                      @RequestBody @Valid ActivityRequestDto requestDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.update(id, requestDto, userDetails));
    }

    @PostMapping("/activities/{id}/publish")
    public ResponseEntity<ActivityResponseDto> publish(@PathVariable Long id,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.publish(id, userDetails));
    }

    @PostMapping("/activities/{id}/cancel")
    public ResponseEntity<ActivityResponseDto> cancel(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.cancel(id, userDetails));
    }

    @PostMapping("/activities/{id}/complete")
    public ResponseEntity<ActivityResponseDto> complete(@PathVariable Long id,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.complete(id, userDetails));
    }

    // Public; the principal is null for anonymous callers.
    @GetMapping("/activities/{id}")
    public ResponseEntity<ActivityResponseDto> get(@PathVariable Long id,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.get(id, userDetails));
    }

    // Spring validates the constrained parameters itself and answers 400.
    @GetMapping("/activities")
    public ResponseEntity<ActivityPageResponseDto> listOpen(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(activityService.listOpen(page, size));
    }

    @GetMapping("/organizations/me/activities")
    public ResponseEntity<List<ActivityResponseDto>> listMine(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityService.listMine(userDetails));
    }
}
