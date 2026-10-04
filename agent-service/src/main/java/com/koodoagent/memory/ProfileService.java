package com.koodoagent.memory;

import com.koodoagent.dto.ConceptProfileDTO;
import com.koodoagent.persistence.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    public static final int MIN_FAMILIARITY = 0;
    public static final int MAX_FAMILIARITY = 3;

    private final ProfileRepository profileRepository;


    /**
     * 用户问了一个概念时调用：确保 profile 行存在，并累加 ask_count。
     * 不修改 familiarity。
     */
    public void recordAsk(String conceptName) {
        if (conceptName == null || conceptName.isBlank()) return;
        profileRepository.upsertOnAsk(conceptName.trim());
    }


    public void resetConcept(String conceptName) {
        if (conceptName == null || conceptName.isBlank()) return;
        profileRepository.deleteByConceptName(conceptName.trim());
        profileRepository.deleteFeedbackByTerm(conceptName.trim());
    }

    public Optional<ConceptProfileDTO> get(String conceptName) {
        if (conceptName == null || conceptName.isBlank()) return Optional.empty();
        return profileRepository.findByConceptName(conceptName.trim());
    }

    /**
     * 获取当前熟悉度；不存在则视为 0（完全陌生），不写库。
     */
    public int getFamiliarity(String conceptName) {
        return get(conceptName)
                .map(ConceptProfileDTO::familiarityLevel)
                .orElse(MIN_FAMILIARITY);
    }

    /**
     * 手动设置熟悉度，自动裁剪到 [0,3] 范围。
     */
    public void setFamiliarity(String conceptName, int level) {
        if (conceptName == null || conceptName.isBlank()) return;
        int clamped = Math.max(MIN_FAMILIARITY, Math.min(MAX_FAMILIARITY, level));
        profileRepository.updateFamiliarity(conceptName.trim(), clamped);
    }

    /**
     * 按 delta 增减熟悉度，内部裁剪。
     */
    public void adjustFamiliarity(String conceptName, int delta) {
        int current = getFamiliarity(conceptName);
        setFamiliarity(conceptName, current + delta);
    }

    /**
     * 反馈计数 +1。今天是底层 API，Day 3 才会按规则调用。
     */
    public void recordFeedbackCounter(String conceptName, String column) {
        if (conceptName == null || conceptName.isBlank()) return;
        profileRepository.incrementFeedbackCounter(conceptName.trim(), column);
    }

    public List<ConceptProfileDTO> listAll() {
        return profileRepository.findAll();
    }

    /**
     * 首次接触某概念时建行，用于测试和手动初始化。
     */
    public void ensureExists(String conceptName) {
        if (conceptName == null || conceptName.isBlank()) return;
        if (get(conceptName).isEmpty()) {
            profileRepository.upsertOnAsk(conceptName.trim());
            log.info("ProfileService 首次建档: concept={}", conceptName);
        }
    }
}