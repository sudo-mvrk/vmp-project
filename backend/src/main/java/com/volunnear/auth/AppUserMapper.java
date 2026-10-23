package com.volunnear.auth;

import com.volunnear.auth.dto.AppUserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashSet;
import java.util.Set;

@Mapper(componentModel = "spring", imports = {HashSet.class, Set.class})
public interface AppUserMapper {

    @Mapping(target = "roles", source = "roles")
    AppUserResponseDto toDto(AppUser appUser);
}
