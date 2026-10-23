package com.volunnear.volunteer;

import com.volunnear.volunteer.dto.VolunteerUpdateProfileRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Capacity (Q3e), radius (Q3g) and location (Q2b) rules of the update DTO. */
class VolunteerUpdateProfileValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void negativeCapacityIsInvalid() {
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().maxActiveAssignments(-1).build()))
                .containsExactly("maxActiveAssignments");
    }

    @Test
    void capacityAboveTenIsInvalid() {
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().maxActiveAssignments(11).build()))
                .containsExactly("maxActiveAssignments");
    }

    @Test
    void capacityZeroAndTenAreValid() {
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().maxActiveAssignments(0).build())).isEmpty();
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().maxActiveAssignments(10).build())).isEmpty();
    }

    @Test
    void radiusOutsideOneTo500IsInvalid() {
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().radius(0).build())).containsExactly("radius");
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().radius(501).build())).containsExactly("radius");
    }

    @Test
    void onlyOneCoordinateIsInvalid() {
        assertThat(invalidFields(VolunteerUpdateProfileRequestDto.builder().lat(50.0).build())).containsExactly("location");
    }

    private static Set<String> invalidFields(VolunteerUpdateProfileRequestDto dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
