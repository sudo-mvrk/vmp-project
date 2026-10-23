package com.volunnear.organization;

import com.volunnear.auth.CustomUserDetails;
import com.volunnear.organization.dto.OrganizationProfileResponseDto;
import com.volunnear.organization.dto.OrganizationUpdateProfileRequestDto;
import com.volunnear.organization.dto.OwnOrganizationProfileResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationProfileResponseDto> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganization(id));
    }

    @GetMapping("/me")
    public ResponseEntity<OwnOrganizationProfileResponseDto> getMyOrganization(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(organizationService.getMyOrganization(userDetails));
    }

    @PutMapping("/me")
    public ResponseEntity<OwnOrganizationProfileResponseDto> updateMyOrganization(@RequestBody @Valid OrganizationUpdateProfileRequestDto requestDto,
                                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(organizationService.updateMyOrganization(requestDto, userDetails));
    }
}
