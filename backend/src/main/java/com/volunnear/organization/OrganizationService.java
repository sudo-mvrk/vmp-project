package com.volunnear.organization;

import com.volunnear.auth.AppUser;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.organization.dto.OrganizationProfileResponseDto;
import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.organization.dto.OrganizationUpdateProfileRequestDto;
import com.volunnear.organization.dto.OwnOrganizationProfileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationMapper organizationMapper;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationAccess organizationAccess;

    /** Creates the profile and its single OWNER member in the registration transaction. */
    @Transactional
    public void createOrganizationProfile(OrganizationRegistrationRequestDto request, AppUser appUser) {
        Organization organization = organizationRepository.save(organizationMapper.toEntity(request));
        memberRepository.save(OrganizationMember.owner(organization.getId(), appUser.getId()));
    }

    @Transactional(readOnly = true)
    public OrganizationProfileResponseDto getOrganization(Long organizationId) {
        return organizationMapper.toDto(findOrganization(organizationId));
    }

    @Transactional(readOnly = true)
    public OwnOrganizationProfileResponseDto getMyOrganization(CustomUserDetails userDetails) {
        return toOwnDto(findOrganization(myOrganizationId(userDetails)), userDetails);
    }

    @Transactional
    public OwnOrganizationProfileResponseDto updateMyOrganization(OrganizationUpdateProfileRequestDto request,
                                                               CustomUserDetails userDetails) {
        // The organization is found through the caller's own membership, so no further ownership check is needed.
        Organization organization = findOrganization(myOrganizationId(userDetails));
        organizationMapper.updateEntity(request, organization);
        return toOwnDto(organization, userDetails);
    }

    /** The organization the caller belongs to; 404 when the account has none. Used by other features. */
    @Transactional(readOnly = true)
    public Organization getMyOrganizationEntity(CustomUserDetails userDetails) {
        return findOrganization(myOrganizationId(userDetails));
    }

    private Long myOrganizationId(CustomUserDetails userDetails) {
        return organizationAccess.organizationIdOf(userDetails.getUser().getId())
                .orElseThrow(() -> new DataNotFoundException(
                        "Organization profile of user " + userDetails.getUsername() + " not found"));
    }

    private OwnOrganizationProfileResponseDto toOwnDto(Organization organization, CustomUserDetails userDetails) {
        return new OwnOrganizationProfileResponseDto(userDetails.getUsername(), organizationMapper.toDto(organization));
    }

    private Organization findOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new DataNotFoundException("Organization " + organizationId + " not found"));
    }
}
