package com.volunnear.organization;

import com.volunnear.organization.dto.OrganizationProfileResponseDto;
import com.volunnear.organization.dto.OrganizationRegistrationRequestDto;
import com.volunnear.organization.dto.OrganizationUpdateProfileRequestDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "websiteUrl", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "fullAddress", ignore = true)
    @Mapping(target = "countryCode", ignore = true)
    Organization toEntity(OrganizationRegistrationRequestDto requestDto);

    // A JTS point stores x = longitude, y = latitude.
    @Mapping(target = "lat", source = "location.y")
    @Mapping(target = "lon", source = "location.x")
    OrganizationProfileResponseDto toDto(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(OrganizationUpdateProfileRequestDto requestDto, @MappingTarget Organization organization);

    // Q2b: both coordinates set the location, both absent keep it; the DTO rejects only one of them.
    @AfterMapping
    default void updateLocation(OrganizationUpdateProfileRequestDto requestDto, @MappingTarget Organization organization) {
        if (requestDto.lat() != null && requestDto.lon() != null) {
            organization.setLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(requestDto.lon(), requestDto.lat())));
        }
    }
}
