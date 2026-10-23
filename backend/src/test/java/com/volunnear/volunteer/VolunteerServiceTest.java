package com.volunnear.volunteer;

import com.volunnear.auth.AppUser;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.common.exception.DataNotFoundException;
import com.volunnear.dictionary.Category;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.DictionaryService;
import com.volunnear.dictionary.Skill;
import com.volunnear.volunteer.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Uses the generated mapper, so the partial-update and location rules (Q2b) are tested for real. */
@ExtendWith(MockitoExtension.class)
class VolunteerServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    private VolunteerRepository repository;
    @Mock
    private DictionaryService dictionaryService;

    private VolunteerService service;
    private CustomUserDetails userDetails;
    private Volunteer volunteer;

    @BeforeEach
    void setUp() {
        service = new VolunteerService(Mappers.getMapper(VolunteerMapper.class), repository, dictionaryService);
        AppUser appUser = AppUser.builder().id(USER_ID).username("vol").build();
        userDetails = new CustomUserDetails(appUser);
        volunteer = Volunteer.builder()
                .id(USER_ID)
                .appUser(appUser)
                .firstName("Vera")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .location(VolunteerMapper.GEOMETRY_FACTORY.createPoint(new Coordinate(16.6, 49.2)))
                .build();
    }

    @Test
    void newProfileHasCapacityOne() {
        // Q3e: the builder default is what registration stores.
        assertThat(volunteer.getMaxActiveAssignments()).isEqualTo(1);
    }

    @Test
    void updateChangesGivenFieldsAndKeepsTheRest() {
        givenMyProfile();

        OwnVolunteerProfileResponseDto response = service.updateMyProfile(
                VolunteerUpdateProfileRequestDto.builder().bio("Likes gardening").maxActiveAssignments(3).build(), userDetails);

        assertThat(response.username()).isEqualTo("vol");
        assertThat(response.profile().bio()).isEqualTo("Likes gardening");
        assertThat(response.profile().maxActiveAssignments()).isEqualTo(3);
        assertThat(response.profile().firstName()).isEqualTo("Vera");
        assertThat(response.profile().dateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        // No lat/lon in the request: the stored location stays (Q2b).
        assertThat(response.profile().lat()).isEqualTo(49.2);
        assertThat(response.profile().lon()).isEqualTo(16.6);
    }

    @Test
    void latitudeZeroIsAValidLocation() {
        givenMyProfile();

        OwnVolunteerProfileResponseDto response = service.updateMyProfile(
                VolunteerUpdateProfileRequestDto.builder().lat(0.0).lon(10.0).build(), userDetails);

        assertThat(response.profile().lat()).isEqualTo(0.0);
        assertThat(response.profile().lon()).isEqualTo(10.0);
    }

    @Test
    void skillsAreReplacedAsAWhole() {
        givenMyProfile();
        volunteer.getSkills().add(skill(9L, "Old skill"));
        when(dictionaryService.requireSkills(Set.of(1L, 2L))).thenReturn(Set.of(skill(1L, "Driving"), skill(2L, "Childcare")));

        OwnVolunteerProfileResponseDto response = service.replaceMySkills(
                new VolunteerSkillsRequestDto(List.of(2L, 1L, 2L)), userDetails);

        assertThat(response.profile().skills()).extracting("skillName").containsExactly("Childcare", "Driving");
    }

    @Test
    void unknownSkillIsRejectedBeforeTheProfileIsTouched() {
        when(dictionaryService.requireSkills(any())).thenThrow(new BadDataInRequestException("Unknown skill id(s): [99]"));

        assertThatThrownBy(() -> service.replaceMySkills(new VolunteerSkillsRequestDto(List.of(99L)), userDetails))
                .isInstanceOf(BadDataInRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void certificationsAreReplacedWithTheirValidity() {
        givenMyProfile();
        LocalDate validUntil = LocalDate.now().plusYears(1);
        when(dictionaryService.requireCertifications(Set.of(5L)))
                .thenReturn(Map.of(5L, new Certification(5L, "First aid certificate")));

        OwnVolunteerProfileResponseDto response = service.replaceMyCertifications(new VolunteerCertificationsRequestDto(
                List.of(new HeldCertificationRequestDto(5L, validUntil))), userDetails);

        assertThat(response.profile().certifications())
                .containsExactly(new HeldCertificationResponseDto(5L, "First aid certificate", validUntil));
    }

    @Test
    void duplicateCertificationIsRejected() {
        assertThatThrownBy(() -> service.replaceMyCertifications(new VolunteerCertificationsRequestDto(List.of(
                new HeldCertificationRequestDto(5L, null), new HeldCertificationRequestDto(5L, null))), userDetails))
                .isInstanceOf(BadDataInRequestException.class)
                .hasMessageContaining("listed twice");
        verifyNoInteractions(dictionaryService);
    }

    @Test
    void availabilityIsReplacedAndSortedByDayAndStart() {
        givenMyProfile();

        OwnVolunteerProfileResponseDto response = service.replaceMyAvailability(new VolunteerAvailabilityRequestDto(List.of(
                slot(DayOfWeek.TUESDAY, "09:00", "12:00"),
                slot(DayOfWeek.MONDAY, "14:00", "18:00"),
                slot(DayOfWeek.MONDAY, "08:00", "10:00"))), userDetails);

        assertThat(response.profile().availability()).containsExactly(
                slot(DayOfWeek.MONDAY, "08:00", "10:00"),
                slot(DayOfWeek.MONDAY, "14:00", "18:00"),
                slot(DayOfWeek.TUESDAY, "09:00", "12:00"));
    }

    @Test
    void overlappingAvailabilityIsRejectedBeforeTheProfileIsTouched() {
        assertThatThrownBy(() -> service.replaceMyAvailability(new VolunteerAvailabilityRequestDto(List.of(
                slot(DayOfWeek.MONDAY, "09:00", "12:00"), slot(DayOfWeek.MONDAY, "11:00", "13:00"))), userDetails))
                .isInstanceOf(BadDataInRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void missingProfileIsNotFound() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(userDetails)).isInstanceOf(DataNotFoundException.class);
    }

    private void givenMyProfile() {
        when(repository.findById(USER_ID)).thenReturn(Optional.of(volunteer));
    }

    private static Skill skill(Long id, String name) {
        return Skill.builder().id(id).skillName(name).category(Category.SOCIAL).build();
    }

    private static AvailabilitySlotDto slot(DayOfWeek day, String start, String end) {
        return new AvailabilitySlotDto(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
