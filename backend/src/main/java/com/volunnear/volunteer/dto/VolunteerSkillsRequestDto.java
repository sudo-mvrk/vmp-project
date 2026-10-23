package com.volunnear.volunteer.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The complete list of skills (Q3c); an empty list clears them. Duplicate ids count once. */
public record VolunteerSkillsRequestDto(@NotNull List<@NotNull Long> skillIds) {
}
