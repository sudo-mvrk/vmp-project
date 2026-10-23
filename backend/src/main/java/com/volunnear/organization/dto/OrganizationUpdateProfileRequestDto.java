package com.volunnear.organization.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

/**
 * Partial update: an absent (null) field keeps the stored value.
 * Location is set only when lat and lon are given together (Q2b).
 */
@Builder
public record OrganizationUpdateProfileRequestDto(
        @Size(max = 255)
        @Pattern(regexp = ".*\\S.*", message = "Organization name must not be blank")
        String organizationName,
        String description,
        @Size(max = 255)
        @URL(message = "Website must be a valid URL")
        String websiteUrl,
        @DecimalMin(value = "-90.0", message = "Latitude can't be less than -90")
        @DecimalMax(value = "90.0", message = "Latitude can't be more than 90")
        Double lat,
        @DecimalMin(value = "-180.0", message = "Longitude can't be less than -180")
        @DecimalMax(value = "180.0", message = "Longitude can't be more than 180")
        Double lon,
        @Size(max = 255)
        String city,
        @Size(max = 255)
        String fullAddress,
        @Pattern(regexp = "[A-Z]{2}", message = "Country code must be two upper-case letters (ISO 3166-1 alpha-2)")
        String countryCode
) {
    // Bean Validation reads this as the property "location"; the error is reported under that name.
    @AssertTrue(message = "lat and lon must be given together")
    public boolean isLocation() {
        return (lat == null) == (lon == null);
    }
}
