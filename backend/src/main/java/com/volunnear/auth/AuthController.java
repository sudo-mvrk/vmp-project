package com.volunnear.auth;

import com.volunnear.auth.dto.LoginRequestDto;
import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.volunteer.dto.VolunteerRegistrationRequestDto;
import com.volunnear.auth.dto.AppUserResponseDto;
import com.volunnear.auth.dto.CsrfTokenResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityFacade securityFacade;

    @PostMapping("/register/volunteer")
    public ResponseEntity<AppUserResponseDto> registerVolunteer(@RequestBody @Valid VolunteerRegistrationRequestDto request) {
        AppUserResponseDto response = authService.registerVolunteer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/register/organization")
    public ResponseEntity<AppUserResponseDto> registerOrganization(@RequestBody @Valid OrganizationRegistrationRequestDto request) {
        AppUserResponseDto response = authService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDto requestDto,
                                      HttpServletRequest httpServletRequest,
                                      HttpServletResponse httpServletResponse) {
        securityFacade.authenticateAndCreateSession(requestDto, httpServletRequest, httpServletResponse);
        return ResponseEntity.ok().build();
    }

    // Spring resolves the CsrfToken argument; reading it creates the token and sets the XSRF-TOKEN cookie.
    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponseDto> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponseDto(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<AppUserResponseDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AppUserResponseDto response = authService.getMyProfile(userDetails);
        return ResponseEntity.ok(response);
    }
}
