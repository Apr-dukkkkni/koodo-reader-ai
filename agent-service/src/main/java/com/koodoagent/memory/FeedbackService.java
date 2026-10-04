package com.koodoagent.memory;

import com.koodoagent.dto.FeedbackRequest;
import com.koodoagent.persistence.FeedbackRepository;
import com.koodoagent.persistence.QaHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


//用户反馈功能的业务逻辑层（Service 层），负责编排整条反馈处理链路：参数校验 → 反馈记录落库 → 反查问答主题 → 更新概念画像计数器。
//它处于 Controller 和 Repository 之间，不直接操作数据库，而是调用底层的 FeedbackRepository、QaHistoryRepository 以及 ProfileService 完成业务，是反馈功能的核心调度类。
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;   //FeedbackRepository：负责向 feedback 表写入用户反馈记录
    private final QaHistoryRepository qaHistoryRepository; //QaHistoryRepository：负责根据 qaId 反查对应的主题关键词 term
    private final ProfileService profileService;           //ProfileService：负责操作 concept_profile 概念画像表，确保行存在、累加反馈计数

    public FeedbackService(FeedbackRepository feedbackRepository,
                           QaHistoryRepository qaHistoryRepository,
                           ProfileService profileService) {
        this.feedbackRepository = feedbackRepository;
        this.qaHistoryRepository = qaHistoryRepository;
        this.profileService = profileService;
    }

    /**
     * 反馈入主流程：
     *   1. 落 feedback 表
     *   2. 用 qaId 反查 term，把 counter 记到 concept_profile
     *      —— 只累加 counter，不动 familiarity（Day 3 才做规则）
     */
    public void record(FeedbackRequest request) {
        validate(request);

        feedbackRepository.save(request.qaId(), request.rating(), request.comment());

        String term = qaHistoryRepository.findTermByQaId(request.qaId());
        if (term == null || term.isBlank()) {
            log.warn("feedback qaId={} 未找到对应 term，跳过 concept_profile 更新", request.qaId());
            return;
        }

        String column = ratingToColumn(request.rating());
        if (column == null) {
            log.warn("feedback rating={} 不识别，跳过 counter 累加", request.rating());
            return;
        }

        profileService.recordAsk(term);
        profileService.recordFeedbackCounter(term, column);
        applyFamiliarityRule(term);
        log.info("feedback 记录: qaId={} term={} rating={}", request.qaId(), term, request.rating());
    }



    /**
     * 连续 2 次同向反馈 → 调整 familiarity。
     * 每次调用都查最近 2 条 feedback，幂等可解释。
     */
    private void applyFamiliarityRule(String term) {
        List<String> recent = feedbackRepository.findRecentRatingsByTerm(term, 2);
        if (recent.size() < 2) {
            return;
        }

        boolean bothShallow = recent.stream().allMatch("TOO_SHALLOW"::equals);
        boolean bothDeep    = recent.stream().allMatch("TOO_DEEP"::equals);

        if (bothShallow) {
            int before = profileService.getFamiliarity(term);
            profileService.adjustFamiliarity(term, +1);
            int after = profileService.getFamiliarity(term);
            log.info("familiarity 因连续太浅 +1: term={} {} -> {}", term, before, after);
        } else if (bothDeep) {
            int before = profileService.getFamiliarity(term);
            profileService.adjustFamiliarity(term, -1);
            int after = profileService.getFamiliarity(term);
            log.info("familiarity 因连续太深 -1: term={} {} -> {}", term, before, after);
        }
    }

    /**
     * 入参校验方法，在执行业务前先拦截非法请求。
     * 校验规则
     * qaId 不能为空、不能是空字符串
     * rating 不能为空、不能是空字符串
     * rating 必须是系统识别的合法值（TOO_SHALLOW / JUST_RIGHT / TOO_DEEP）
     */
    private void validate(FeedbackRequest request) {
        if (request.qaId() == null || request.qaId().isBlank()) {
            throw new IllegalArgumentException("qaId 不能为空");
        }
        if (request.rating() == null || request.rating().isBlank()) {
            throw new IllegalArgumentException("rating 不能为空");
        }
        if (ratingToColumn(request.rating()) == null) {
            throw new IllegalArgumentException("非法 rating: " + request.rating());
        }
    }

    /**
     * 评级→数据库字段名的转换方法，是内部工具方法。
     * @param rating
     * @return
     */
    private String ratingToColumn(String rating) {
        return switch (rating) {
            case "TOO_SHALLOW" -> "too_shallow_count";
            case "JUST_RIGHT"  -> "just_right_count";
            case "TOO_DEEP"    -> "too_deep_count";
            default -> null;
        };
    }
}