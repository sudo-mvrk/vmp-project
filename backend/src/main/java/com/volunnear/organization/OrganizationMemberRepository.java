package com.volunnear.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, OrganizationMember.Id> {
    boolean existsById_OrganizationIdAndId_UserId(Long organizationId, Long userId);

    // At most one row per user (Q2d).
    Optional<OrganizationMember> findById_UserId(Long userId);

    /** Q7c: the address notifications to the organization go to; empty if the owner has no email. */
    @Query("""
            select u.email from OrganizationMember m, AppUser u
            where u.id = m.id.userId
              and m.id.organizationId = :organizationId
              and m.role = com.volunnear.organization.OrganizationMemberRole.OWNER
            """)
    Optional<String> findOwnerEmail(@Param("organizationId") Long organizationId);
}
