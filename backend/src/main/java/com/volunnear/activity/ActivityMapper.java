package com.volunnear.activity;

import com.volunnear.activity.dto.ActivityResponseDto;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.Skill;
import com.volunnear.dictionary.dto.CertificationResponseDto;
import com.volunnear.dictionary.dto.SkillResponseDto;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // A JTS point stores x = longitude, y = latitude.
    @Mapping(target = "organizationId", source = "activity.organization.id")
    @Mapping(target = "lat", source = "activity.location.y")
    @Mapping(target = "lon", source = "activity.location.x")
    ActivityResponseDto toDto(Activity activity, long acceptedCount);

    default List<SkillResponseDto> skillsToDto(Set<Skill> skills) {
        return skills.stream()
                .map(skill -> new SkillResponseDto(skill.getId(), skill.getSkillName(), skill.getCategory()))
                .sorted(Comparator.comparing(SkillResponseDto::skillName))
                .toList();
    }

    default List<CertificationResponseDto> certificationsToDto(Set<Certification> certifications) {
        return certifications.stream()
                .map(certification -> new CertificationResponseDto(certification.getId(), certification.getName()))
                .sorted(Comparator.comparing(CertificationResponseDto::name))
                .toList();
    }
}
