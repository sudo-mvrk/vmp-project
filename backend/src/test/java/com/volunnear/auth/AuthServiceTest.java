package com.volunnear.auth;

import com.volunnear.organization.OrganizationService;
import com.volunnear.volunteer.VolunteerService;

import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.volunteer.dto.VolunteerRegistrationRequestDto;
import com.volunnear.auth.dto.AppUserResponseDto;
import com.volunnear.common.exception.UserAlreadyExistsException;
import com.volunnear.organization.OrganizationRepository;
import com.volunnear.volunteer.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VolunteerService volunteerService;
    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private AuthService authService;
    @Captor
    private ArgumentCaptor<AppUser> userCaptor;

    private VolunteerRegistrationRequestDto volunteerRequest;
    private OrganizationRegistrationRequestDto organizationRequest;

    @BeforeEach
    void prepareData() {
        this.volunteerRequest = new VolunteerRegistrationRequestDto(
                "test",
                "test",
                "test@gmail.com",
                "test",
                "test",
                LocalDate.of(2003, 3, 2));
        this.organizationRequest = new OrganizationRegistrationRequestDto(
                "test",
                "test",
                "test@gmail.com",
                "Test Name"
        );
    }

    // Volunteer tests
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExistsVolunteerRequest() {
        when(appUserRepository.existsByEmail(volunteerRequest.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerVolunteer(volunteerRequest));

        verify(appUserRepository, never()).save(any());
        verify(volunteerRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        when(appUserRepository.existsByUsername(volunteerRequest.username())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerVolunteer(volunteerRequest));

        verify(appUserRepository, never()).save(any());
        verify(volunteerRepository, never()).save(any());
    }


    // Organization tests
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExistsOrganizationRequest() {
        when(appUserRepository.existsByEmail(organizationRequest.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerOrganization(organizationRequest));

        verify(appUserRepository, never()).save(any());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExistsOrganizationRequest() {
        when(appUserRepository.existsByUsername(organizationRequest.username())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerOrganization(organizationRequest));

        verify(appUserRepository, never()).save(any());
        verify(organizationRepository, never()).save(any());
    }

    @Nested
    class SuccessfullyRegistration {
        String encodedPassMock;

        @BeforeEach
        void setUpSuccessMocks() {
            encodedPassMock = "encodedPassword!@#";
            when(appUserRepository.existsByEmail(volunteerRequest.email())).thenReturn(false);
            when(appUserRepository.existsByUsername(volunteerRequest.username())).thenReturn(false);
            when(passwordEncoder.encode(volunteerRequest.password())).thenReturn(encodedPassMock);

            AppUser appUser = new AppUser();
            appUser.setId(42L);
            when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
                AppUser userToSave = invocation.getArgument(0);
                userToSave.setId(42L);
                return userToSave;
            });

            when(appUserMapper.toDto(any(AppUser.class))).thenAnswer(invocation -> {
                AppUser passedUser = invocation.getArgument(0);

                List<String> mappedRoles = passedUser.getRoles().stream()
                        .map(Role::name)
                        .toList();

                return new AppUserResponseDto(
                        passedUser.getId(),
                        passedUser.getEmail(),
                        passedUser.getUsername(),
                        mappedRoles
                );
            });
        }

        @Test
        void shouldSuccessfullyCreateVolunteer() {
            AppUserResponseDto result = authService.registerVolunteer(volunteerRequest);
            verify(appUserRepository).save(userCaptor.capture());
            AppUser capturedUser = userCaptor.getValue();

            assertEquals(volunteerRequest.email(), capturedUser.getEmail());
            assertEquals(encodedPassMock, capturedUser.getPassword());
            assertTrue(capturedUser.getRoles().contains(Role.ROLE_VOLUNTEER));
            verify(volunteerService).createVolunteerProfile(volunteerRequest, capturedUser);

            assertNotNull(result);
            assertEquals(volunteerRequest.email(), result.email());
            assertTrue(result.roles().contains(Role.ROLE_VOLUNTEER.name()));
        }

        @Test
        void shouldSuccessfullyCreateOrganization() {
            AppUserResponseDto result = authService.registerOrganization(organizationRequest);
            verify(appUserRepository).save(userCaptor.capture());
            AppUser capturedUser = userCaptor.getValue();

            assertEquals(organizationRequest.email(), capturedUser.getEmail());
            assertEquals(encodedPassMock, capturedUser.getPassword());
            assertTrue(capturedUser.getRoles().contains(Role.ROLE_ORGANIZATION));
            verify(organizationService).createOrganizationProfile(organizationRequest, capturedUser);

            assertNotNull(result);
            assertEquals(organizationRequest.email(), result.email());
            assertTrue(result.roles().contains(Role.ROLE_ORGANIZATION.name()));
        }
    }
}