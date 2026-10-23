package com.volunnear.dictionary;

import com.volunnear.common.exception.BadDataInRequestException;
import com.volunnear.dictionary.dto.CertificationResponseDto;
import com.volunnear.dictionary.dto.SkillResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;

    @Transactional(readOnly = true)
    public List<SkillResponseDto> getSkills() {
        return skillRepository.findAll(Sort.by("category", "skillName")).stream()
                .map(skill -> new SkillResponseDto(skill.getId(), skill.getSkillName(), skill.getCategory()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificationResponseDto> getCertifications() {
        return certificationRepository.findAll(Sort.by("name")).stream()
                .map(certification -> new CertificationResponseDto(certification.getId(), certification.getName()))
                .toList();
    }

    /** The skills with these ids; an unknown id is 400. */
    @Transactional(readOnly = true)
    public Set<Skill> requireSkills(Collection<Long> ids) {
        return new HashSet<>(requireAll(ids, skillRepository.findAllById(ids), Skill::getId, "skill"));
    }

    /** The certifications with these ids, by id; an unknown id is 400. */
    @Transactional(readOnly = true)
    public Map<Long, Certification> requireCertifications(Collection<Long> ids) {
        return requireAll(ids, certificationRepository.findAllById(ids), Certification::getId, "certification").stream()
                .collect(Collectors.toMap(Certification::getId, Function.identity()));
    }

    private static <T> List<T> requireAll(Collection<Long> ids, List<T> found, Function<T, Long> idOf, String kind) {
        Set<Long> missing = new HashSet<>(ids);
        found.forEach(entry -> missing.remove(idOf.apply(entry)));
        if (!missing.isEmpty()) {
            throw new BadDataInRequestException("Unknown " + kind + " id(s): " + missing.stream().sorted().toList());
        }
        return found;
    }
}
