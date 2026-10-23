package com.volunnear.application;

import com.volunnear.application.dto.ActivityApplicationResponseDto;
import com.volunnear.application.dto.HistoryItemResponseDto;
import com.volunnear.application.dto.InvitationRequestDto;
import com.volunnear.application.dto.MyApplicationResponseDto;
import com.volunnear.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping("/activities/{activityId}/applications")
    public ResponseEntity<MyApplicationResponseDto> apply(@PathVariable Long activityId,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(activityId, userDetails));
    }

    @GetMapping("/activities/{activityId}/applications")
    public ResponseEntity<List<ActivityApplicationResponseDto>> listForActivity(@PathVariable Long activityId,
                                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.listForActivity(activityId, userDetails));
    }

    @PostMapping("/applications/{id}/accept")
    public ResponseEntity<ActivityApplicationResponseDto> accept(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.accept(id, userDetails));
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<ActivityApplicationResponseDto> reject(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.reject(id, userDetails));
    }

    @PostMapping("/applications/{id}/withdraw")
    public ResponseEntity<MyApplicationResponseDto> withdraw(@PathVariable Long id,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.withdraw(id, userDetails));
    }

    // Q5m: invitations are applications with origin INVITATION; they appear in both lists above and below.
    @PostMapping("/activities/{activityId}/invitations")
    public ResponseEntity<ActivityApplicationResponseDto> invite(@PathVariable Long activityId,
                                                                 @RequestBody @Valid InvitationRequestDto requestDto,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.invite(activityId, requestDto, userDetails));
    }

    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<MyApplicationResponseDto> acceptInvitation(@PathVariable Long id,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.acceptInvitation(id, userDetails));
    }

    @PostMapping("/invitations/{id}/decline")
    public ResponseEntity<MyApplicationResponseDto> declineInvitation(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.declineInvitation(id, userDetails));
    }

    @GetMapping("/volunteers/me/history")
    public ResponseEntity<List<HistoryItemResponseDto>> history(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.history(userDetails));
    }

    @GetMapping("/volunteers/me/applications")
    public ResponseEntity<List<MyApplicationResponseDto>> listMine(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.listMine(userDetails));
    }
}
