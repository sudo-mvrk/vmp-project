package com.volunnear.organization;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

/**
 * Organization profile. Accounts are linked to it through {@link OrganizationMember}, not by a shared id (Q-E).
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String organizationName;
    @Column(columnDefinition = "text")
    private String description;
    private String websiteUrl;
    private Point location;
    private String city;
    private String fullAddress;
    private String countryCode;
}
