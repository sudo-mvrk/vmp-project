package com.volunnear.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The single place that decides whether an account may act for an organization (Q-E).
 * Every ownership rule goes through it; no service compares user and organization ids itself.
 */
@Component
@RequiredArgsConstructor
public class OrganizationAccess {
    private final OrganizationMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public boolean canManage(Long userId, Long organizationId) {
        return memberRepository.existsById_OrganizationIdAndId_UserId(organizationId, userId);
    }

    /** Like {@link #canManage}, but throws Spring's {@link AccessDeniedException}, which is answered with 403. */
    public void requireManage(Long userId, Long organizationId) {
        if (!canManage(userId, organizationId)) {
            throw new AccessDeniedException("You cannot manage this organization");
        }
    }

    /** The organization the account belongs to, if any. */
    @Transactional(readOnly = true)
    public Optional<Long> organizationIdOf(Long userId) {
        return memberRepository.findById_UserId(userId).map(member -> member.getId().organizationId());
    }
}
