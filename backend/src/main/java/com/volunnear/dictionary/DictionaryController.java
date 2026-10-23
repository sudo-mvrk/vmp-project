package com.volunnear.dictionary;

import com.volunnear.dictionary.dto.CertificationResponseDto;
import com.volunnear.dictionary.dto.SkillResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DictionaryController {
    private final DictionaryService dictionaryService;

    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponseDto>> getSkills() {
        return ResponseEntity.ok(dictionaryService.getSkills());
    }

    @GetMapping("/certifications")
    public ResponseEntity<List<CertificationResponseDto>> getCertifications() {
        return ResponseEntity.ok(dictionaryService.getCertifications());
    }
}
