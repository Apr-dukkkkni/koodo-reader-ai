package com.koodoagent.dto;

/**
 * concept_profile 表的一行映射。
 * 用 record 保持不可变。
 */
public record ConceptProfileDTO(
        Long id,
        String conceptName,
        int familiarityLevel,
        int askCount,
        int tooShallowCount,
        int justRightCount,
        int tooDeepCount,
        String lastSeen
) {
}