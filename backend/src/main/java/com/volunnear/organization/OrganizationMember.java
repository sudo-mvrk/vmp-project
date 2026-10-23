package com.volunnear.organization;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Links a user account to an organization (Q-E). Plain ids, no entity relations:
 * only {@link OrganizationAccess} and the service read it.
 */
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization_member")
public class OrganizationMember {
    @EmbeddedId
    private Id id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    public static OrganizationMember owner(Long organizationId, Long userId) {
        return new OrganizationMember(new Id(organizationId, userId), OrganizationMemberRole.OWNER);
    }

    @Embeddable
    public record Id(
            @Column(name = "organization_id") Long organizationId,
            @Column(name = "user_id") Long userId
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
