package com.volunnear.auth;

import com.volunnear.organization.OrganizationService;
import com.volunnear.volunteer.VolunteerService;

import com.volunnear.auth.dto.RegistrationCredentials;
import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.volunteer.dto.VolunteerRegistrationRequestDto;
import com.volunnear.auth.dto.AppUserResponseDto;
import com.volunnear.common.exception.UnauthorizedException;
import com.volunnear.common.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final VolunteerService volunteerService;
    private final OrganizationService organizationService;
    private final AppUserRepository appUserRepository;

    @Transactional
    public AppUserResponseDto registerVolunteer(VolunteerRegistrationRequestDto request) {
        checkIfUserExistsByEmailAndUsername(request.username(), request.email());
        AppUser savedUser = createAndSaveAppUser(
                request,
                Role.ROLE_VOLUNTEER);
        volunteerService.createVolunteerProfile(request, savedUser);
        return appUserMapper.toDto(savedUser);
    }

    @Transactional
    public AppUserResponseDto registerOrganization(OrganizationRegistrationRequestDto request) {
        checkIfUserExistsByEmailAndUsername(request.username(), request.email());
        AppUser savedUser = createAndSaveAppUser(
                request,
                Role.ROLE_ORGANIZATION
        );
        organizationService.createOrganizationProfile(request, savedUser);
        return appUserMapper.toDto(savedUser);
    }

    public AppUserResponseDto getMyProfile(CustomUserDetails userDetails) {
        Long id = userDetails.getUser().getId();
        AppUser appUser = appUserRepository.findAppUserById(id)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return appUserMapper.toDto(appUser);
    }

    private AppUser createAndSaveAppUser(RegistrationCredentials credentials, Role role) {
        AppUser appUser = AppUser.builder()
                .username(credentials.username())
                .email(credentials.email())
                .password(passwordEncoder.encode(credentials.password()))
                // Mutable: Hibernate replaces the collection contents when the user is saved again.
                .roles(EnumSet.of(role))
                .build();
        return appUserRepository.save(appUser);
    }

    private void checkIfUserExistsByEmailAndUsername(String username, String email) {
        if (appUserRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username", "User with that username already exists");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email", "User with that email already exists");
        }
    }
}
