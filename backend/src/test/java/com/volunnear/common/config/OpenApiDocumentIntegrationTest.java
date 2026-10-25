package com.volunnear.common.config;

import com.volunnear.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Q67: the document the frontend types are generated from (`npm run gen:api`). */
class OpenApiDocumentIntegrationTest extends AbstractIntegrationTest {
    private static final String SCHEMAS = "$.components.schemas.";

    @Test
    void responseFieldsAreRequiredAndOnlyAnnotatedOnesNullable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // The own profile, with the fields of the unwrapped public profile.
                .andExpect(jsonPath(SCHEMAS + "OwnVolunteerProfileResponseDto.required",
                        hasItems("username", "id", "lat", "radius", "maxActiveAssignments", "skills", "availability")))
                .andExpect(jsonPath(SCHEMAS + "OwnVolunteerProfileResponseDto.properties.lat.nullable").value(true))
                .andExpect(jsonPath(SCHEMAS + "OwnVolunteerProfileResponseDto.properties.id.nullable").doesNotExist())
                // Schemas only nested in responses count too.
                .andExpect(jsonPath(SCHEMAS + "SkillResponseDto.required", hasItems("id", "skillName", "category")))
                .andExpect(jsonPath(SCHEMAS + "ScoreDto.required", hasItems("geo", "skill", "priority", "total", "weights")))
                .andExpect(jsonPath(SCHEMAS + "HeldCertificationResponseDto.properties.validUntil.nullable").value(true))
                .andExpect(jsonPath(SCHEMAS + "AppUserResponseDto.required", hasItems("id", "username", "roles")))
                // A nullable reference to another schema (no weight override).
                .andExpect(jsonPath(SCHEMAS + "ActivityResponseDto.properties.weights.nullable").value(true))
                .andExpect(jsonPath(SCHEMAS + "ActivityResponseDto.properties.weights.allOf[0].$ref")
                        .value("#/components/schemas/ActivityWeightsDto"))
                // Times are strings in JSON, not objects.
                .andExpect(jsonPath(SCHEMAS + "AvailabilitySlotDto.properties.start.type").value("string"));
    }

    @Test
    void partialUpdateRequestKeepsItsFieldsOptional() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SCHEMAS + "VolunteerUpdateProfileRequestDto.required").doesNotExist())
                .andExpect(jsonPath(SCHEMAS + "LoginRequestDto.required", not(hasItem("id"))));
    }
}
