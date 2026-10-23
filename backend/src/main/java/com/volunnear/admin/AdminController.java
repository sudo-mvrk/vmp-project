package com.volunnear.admin;

import com.volunnear.activity.dto.ActivityResponseDto;
import com.volunnear.admin.dto.AdminUserPageResponseDto;
import com.volunnear.admin.dto.AdminUserResponseDto;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.auth.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Platform administration, only for {@code ROLE_ADMIN} (Q32). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<AdminUserPageResponseDto> listUsers(@RequestParam(required = false) String query,
                                                              @RequestParam(required = false) Role role,
                                                              @RequestParam(required = false) Boolean banned,
                                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminService.listUsers(query, role, banned, page, size, userDetails));
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<AdminUserResponseDto> ban(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminService.ban(id, userDetails));
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<AdminUserResponseDto> unban(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminService.unban(id, userDetails));
    }

    @PostMapping("/activities/{id}/cancel")
    public ResponseEntity<ActivityResponseDto> cancelActivity(@PathVariable Long id,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminService.cancelActivity(id, userDetails));
    }
}
