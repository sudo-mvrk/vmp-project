package com.volunnear.organization;

import com.volunnear.auth.AppUser;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.organization.dto.OrganizationProfileResponseDto;
import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.organization.dto.OrganizationUpdateProfileRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Uses the generated mapper, so the partial-update and location rules (Q2b) are tested for real. */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {
    private static final Long ORGANIZATION_ID = 10L;
    private static final Long USER_ID = 1L;

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationMemberRepository memberRepository;
    @Mock
    private OrganizationAccess organizationAccess;

    private OrganizationService service;
    private CustomUserDetails owner;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new OrganizationService(Mappers.getMapper(OrganizationMapper.class),
                organizationRepository, memberRepository, organizationAccess);
        owner = new CustomUserDetails(AppUser.builder().id(USER_ID).username("org").build());

        organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setOrganizationName("Old name");
        organization.setCity("Brno");
        organization.setLocation(OrganizationMapper.GEOMETRY_FACTORY.createPoint(new Coordinate(16.6, 49.2)));
    }

    @Test
    void registrationCreatesProfileAndOwnerMember() {
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setId(ORGANIZATION_ID);
            return saved;
        });

        service.createOrganizationProfile(new OrganizationRegistrationRequestDto("org", "secret", "org@example.com", "Org"),
                AppUser.builder().id(USER_ID).build());

        ArgumentCaptor<OrganizationMember> member = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(memberRepository).save(member.capture());
        assertThat(member.getValue().getId()).isEqualTo(new OrganizationMember.Id(ORGANIZATION_ID, USER_ID));
        assertThat(member.getValue().getRole()).isEqualTo(OrganizationMemberRole.OWNER);
    }

    @Test
    void updateChangesGivenFieldsAndKeepsTheRest() {
        givenOwnOrganization();

        OrganizationProfileResponseDto response = service.updateMyOrganization(
                OrganizationUpdateProfileRequestDto.builder().organizationName("New name").build(), owner).profile();

        assertThat(response.organizationName()).isEqualTo("New name");
        assertThat(response.city()).isEqualTo("Brno");
        // No lat/lon in the request: the stored location stays (Q2b).
        assertThat(response.lat()).isEqualTo(49.2);
        assertThat(response.lon()).isEqualTo(16.6);
    }

    @Test
    void updateWithBothCoordinatesSetsLocationAndLatitudeZeroIsValid() {
        givenOwnOrganization();

        OrganizationProfileResponseDto response = service.updateMyOrganization(
                OrganizationUpdateProfileRequestDto.builder().lat(0.0).lon(-0.5).build(), owner).profile();

        assertThat(response.lat()).isEqualTo(0.0);
        assertThat(response.lon()).isEqualTo(-0.5);
        assertThat(organization.getLocation().getSRID()).isEqualTo(4326);
    }

    @Test
    void accountWithoutMembershipGetsNotFound() {
        when(organizationAccess.organizationIdOf(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrganization(owner)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void unknownOrganizationIsNotFound() {
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrganization(99L)).isInstanceOf(DataNotFoundException.class);
    }

    private void givenOwnOrganization() {
        when(organizationAccess.organizationIdOf(USER_ID)).thenReturn(Optional.of(ORGANIZATION_ID));
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
    }
}
