package com.volunnear.dictionary.dto;

import com.volunnear.dictionary.Category;

public record SkillResponseDto(Long id, String skillName, Category category) {
}
