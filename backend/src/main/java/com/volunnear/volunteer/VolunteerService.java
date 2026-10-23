package com.volunnear.volunteer;

import com.volunnear.auth.AppUser;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.DictionaryService;
import com.volunnear.dictionary.Skill;
import com.volunnear.volunteer.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The caller's own volunteer profile. Every method works on the profile of the logged-in account,
 * so a volunteer can never reach another volunteer's profile.
 */
@Service
@RequiredArgsConstructor
public class VolunteerService {
    private final VolunteerMapper volunteerMapper;
    private final VolunteerRepository volunteerRepository;
    private final DictionaryService dictionaryService;

    @Transactional
    public void createVolunteerProfile(VolunteerRegistrationRequestDto request, AppUser appUser) {
        Volunteer volunteer = volunteerMapper.toEntity(request, appUser);
        volunteerRepository.save(volunteer);
    }

    @Transactional(readOnly = true)
    public OwnVolunteerProfileResponseDto getMyProfile(CustomUserDetails userDetails) {
        return toOwnDto(findMyProfile(userDetails), userDetails);
    }

    @Transactional
    public OwnVolunteerProfileResponseDto updateMyProfile(VolunteerUpdateProfileRequestDto request, CustomUserDetails userDetails) {
        Volunteer volunteer = findMyProfile(userDetails);
        volunteerMapper.updateEntity(request, volunteer);
        return toOwnDto(volunteer, userDetails);
    }

    @Transactional
    public OwnVolunteerProfileResponseDto replaceMySkills(VolunteerSkillsRequestDto request, CustomUserDetails userDetails) {
        // Resolve the ids first, so an unknown id leaves the profile untouched.
        Set<Skill> skills = dictionaryService.requireSkills(new HashSet<>(request.skillIds()));

        Volunteer volunteer = findMyProfile(userDetails);
        volunteer.getSkills().clear();
        volunteer.getSkills().addAll(skills);
        return toOwnDto(volunteer, userDetails);
    }

    @Transactional
    public OwnVolunteerProfileResponseDto replaceMyCertifications(VolunteerCertificationsRequestDto request,
                                                                  CustomUserDetails userDetails) {
        List<HeldCertificationRequestDto> held = request.certifications();
        Set<Long> ids = new HashSet<>();
        held.forEach(entry -> {
            if (!ids.add(entry.certificationId())) {
                throw new BadDataInRequestException("Certification " + entry.certificationId() + " is listed twice");
            }
        });
        Map<Long, Certification> certifications = dictionaryService.requireCertifications(ids);

        Volunteer volunteer = findMyProfile(userDetails);
        volunteer.getCertifications().clear();
        held.forEach(entry -> volunteer.getCertifications().add(
                new VolunteerCertification(certifications.get(entry.certificationId()), entry.validUntil())));
        return toOwnDto(volunteer, userDetails);
    }

    @Transactional
    public OwnVolunteerProfileResponseDto replaceMyAvailability(VolunteerAvailabilityRequestDto request,
                                                                CustomUserDetails userDetails) {
        AvailabilityRules.validate(request.slots());

        Volunteer volunteer = findMyProfile(userDetails);
        volunteer.getAvailability().clear();
        request.slots().forEach(slot -> volunteer.getAvailability().add(
                new VolunteerAvailability(slot.dayOfWeek(), slot.start(), slot.end())));
        return toOwnDto(volunteer, userDetails);
    }

    private Volunteer findMyProfile(CustomUserDetails userDetails) {
        return volunteerRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new DataNotFoundException("Volunteer profile of user " + userDetails.getUsername() + " not found"));
    }

    private OwnVolunteerProfileResponseDto toOwnDto(Volunteer volunteer, CustomUserDetails userDetails) {
        return new OwnVolunteerProfileResponseDto(userDetails.getUsername(), volunteerMapper.toDto(volunteer));
    }
}
