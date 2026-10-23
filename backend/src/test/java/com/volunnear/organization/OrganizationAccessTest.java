package com.volunnear.organization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessTest {
    private static final Long ORGANIZATION = 10L;
    private static final Long OTHER_ORGANIZATION = 20L;
    private static final Long OWNER = 1L;
    private static final Long OTHER_OWNER = 2L;
    private static final Long VOLUNTEER = 3L;

    @Mock
    private OrganizationMemberRepository memberRepository;
    @InjectMocks
    private OrganizationAccess access;

    @Test
    void ownerCanManageOwnOrganization() {
        when(memberRepository.existsById_OrganizationIdAndId_UserId(ORGANIZATION, OWNER)).thenReturn(true);

        assertThat(access.canManage(OWNER, ORGANIZATION)).isTrue();
    }

    @Test
    void ownerOfAnotherOrganizationCannotManage() {
        when(memberRepository.existsById_OrganizationIdAndId_UserId(ORGANIZATION, OTHER_OWNER)).thenReturn(false);

        assertThat(access.canManage(OTHER_OWNER, ORGANIZATION)).isFalse();
    }

    @Test
    void volunteerCannotManage() {
        when(memberRepository.existsById_OrganizationIdAndId_UserId(ORGANIZATION, VOLUNTEER)).thenReturn(false);

        assertThat(access.canManage(VOLUNTEER, ORGANIZATION)).isFalse();
    }

    @Test
    void organizationOfAccountComesFromMembership() {
        when(memberRepository.findById_UserId(OTHER_OWNER))
                .thenReturn(Optional.of(OrganizationMember.owner(OTHER_ORGANIZATION, OTHER_OWNER)));
        when(memberRepository.findById_UserId(VOLUNTEER)).thenReturn(Optional.empty());

        assertThat(access.organizationIdOf(OTHER_OWNER)).contains(OTHER_ORGANIZATION);
        assertThat(access.organizationIdOf(VOLUNTEER)).isEmpty();
    }
}
