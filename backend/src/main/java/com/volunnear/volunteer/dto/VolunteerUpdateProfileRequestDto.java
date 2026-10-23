package com.volunnear.volunteer.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Partial update of the basic profile: an absent (null) field keeps the stored value.
 * Location is set only when lat and lon are given together (Q2b).
 */
@Builder
public record VolunteerUpdateProfileRequestDto(
        @Size(max = 255)
        @Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
        String firstName,
        @Size(max = 255)
        @Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
        String lastName,
        @Past(message = "Date of birth should be in the past")
        LocalDate dateOfBirth,
        String bio,
        @DecimalMin(value = "-90.0", message = "Latitude can't be less than -90")
        @DecimalMax(value = "90.0", message = "Latitude can't be more than 90")
        Double lat,
        @DecimalMin(value = "-180.0", message = "Longitude can't be less than -180")
        @DecimalMax(value = "180.0", message = "Longitude can't be more than 180")
        Double lon,
        @Size(max = 255)
        String locationName,
        @Pattern(regexp = "[A-Z]{2}", message = "Country code must be two upper-case letters (ISO 3166-1 alpha-2)")
        String countryCode,
        @Size(max = 255)
        String city,
        @Size(max = 255)
        String region,
        @Min(value = 1, message = "Radius is at least 1 km")
        @Max(value = 500, message = "Radius is at most 500 km")
        Integer radius,
        @Min(value = 0, message = "Capacity can't be negative")
        @Max(value = 10, message = "Capacity is at most 10")
        Integer maxActiveAssignments
) {
    // Bean Validation reads this as the property "location"; the error is reported under that name.
    @AssertTrue(message = "lat and lon must be given together")
    public boolean isLocation() {
        return (lat == null) == (lon == null);
    }
}
