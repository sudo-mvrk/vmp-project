package com.volunnear.volunteer;

import com.volunnear.auth.AppUser;
import com.volunnear.dictionary.Skill;
import com.volunnear.dictionary.dto.SkillResponseDto;
import com.volunnear.volunteer.dto.AvailabilitySlotDto;
import com.volunnear.volunteer.dto.HeldCertificationResponseDto;
import com.volunnear.volunteer.dto.VolunteerProfileResponseDto;
import com.volunnear.volunteer.dto.VolunteerRegistrationRequestDto;
import com.volunnear.volunteer.dto.VolunteerUpdateProfileRequestDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface VolunteerMapper {
    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appUser", source = "appUser")
    @Mapping(target = "dateOfBirth", source = "requestDto.dateOfBirth")
    @Mapping(target = "firstName", source = "requestDto.firstName")
    @Mapping(target = "lastName", source = "requestDto.lastName")
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "locationName", ignore = true)
    @Mapping(target = "countryCode", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "region", ignore = true)
    @Mapping(target = "radius", ignore = true)
    @Mapping(target = "maxActiveAssignments", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "certifications", ignore = true)
    @Mapping(target = "availability", ignore = true)
    Volunteer toEntity(VolunteerRegistrationRequestDto requestDto, AppUser appUser);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "certifications", ignore = true)
    @Mapping(target = "availability", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(VolunteerUpdateProfileRequestDto requestDto, @MappingTarget Volunteer volunteer);

    // Q2b: both coordinates set the location, both absent keep it; the DTO rejects only one of them.
    @AfterMapping
    default void updateLocation(VolunteerUpdateProfileRequestDto requestDto, @MappingTarget Volunteer volunteer) {
        if (requestDto.lat() != null && requestDto.lon() != null) {
            volunteer.setLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(requestDto.lon(), requestDto.lat())));
        }
    }

    // A JTS point stores x = longitude, y = latitude.
    @Mapping(target = "lat", source = "location.y")
    @Mapping(target = "lon", source = "location.x")
    VolunteerProfileResponseDto toDto(Volunteer volunteer);

    default List<SkillResponseDto> skillsToDto(Set<Skill> skills) {
        return skills.stream()
                .map(skill -> new SkillResponseDto(skill.getId(), skill.getSkillName(), skill.getCategory()))
                .sorted(Comparator.comparing(SkillResponseDto::skillName))
                .toList();
    }

    default List<HeldCertificationResponseDto> certificationsToDto(List<VolunteerCertification> certifications) {
        return certifications.stream()
                .map(held -> new HeldCertificationResponseDto(
                        held.getCertification().getId(), held.getCertification().getName(), held.getValidUntil()))
                .sorted(Comparator.comparing(HeldCertificationResponseDto::name))
                .toList();
    }

    default List<AvailabilitySlotDto> availabilityToDto(List<VolunteerAvailability> availability) {
        return availability.stream()
                .map(slot -> new AvailabilitySlotDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()))
                .sorted(Comparator.comparing(AvailabilitySlotDto::dayOfWeek).thenComparing(AvailabilitySlotDto::start))
                .toList();
    }
}
