package com.volunnear.volunteer;

import com.volunnear.auth.CustomUserDetails;
import com.volunnear.volunteer.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/volunteers/me")
public class VolunteerController {
    private final VolunteerService volunteerService;

    @GetMapping
    public ResponseEntity<OwnVolunteerProfileResponseDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(volunteerService.getMyProfile(userDetails));
    }

    @PutMapping
    public ResponseEntity<OwnVolunteerProfileResponseDto> updateMyProfile(@RequestBody @Valid VolunteerUpdateProfileRequestDto requestDto,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(volunteerService.updateMyProfile(requestDto, userDetails));
    }

    @PutMapping("/skills")
    public ResponseEntity<OwnVolunteerProfileResponseDto> replaceMySkills(@RequestBody @Valid VolunteerSkillsRequestDto requestDto,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(volunteerService.replaceMySkills(requestDto, userDetails));
    }

    @PutMapping("/certifications")
    public ResponseEntity<OwnVolunteerProfileResponseDto> replaceMyCertifications(@RequestBody @Valid VolunteerCertificationsRequestDto requestDto,
                                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(volunteerService.replaceMyCertifications(requestDto, userDetails));
    }

    @PutMapping("/availability")
    public ResponseEntity<OwnVolunteerProfileResponseDto> replaceMyAvailability(@RequestBody @Valid VolunteerAvailabilityRequestDto requestDto,
                                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(volunteerService.replaceMyAvailability(requestDto, userDetails));
    }
}
