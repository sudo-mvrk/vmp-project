package com.volunnear.volunteer.dto;

import com.volunnear.dictionary.dto.SkillResponseDto;

import java.time.LocalDate;
import java.util.List;

/** A volunteer profile without account data (Q3a); the owner sees it inside {@link OwnVolunteerProfileResponseDto}. */
public record VolunteerProfileResponseDto(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String bio,
        Double lat,
        Double lon,
        String locationName,
        String countryCode,
        String city,
        String region,
        Integer radius,
        int maxActiveAssignments,
        List<SkillResponseDto> skills,
        List<HeldCertificationResponseDto> certifications,
        List<AvailabilitySlotDto> availability
) {
}
