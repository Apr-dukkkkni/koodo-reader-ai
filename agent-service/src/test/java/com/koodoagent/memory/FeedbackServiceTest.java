package com.koodoagent.memory;

import com.koodoagent.dto.FeedbackRequest;
import com.koodoagent.persistence.FeedbackRepository;
import com.koodoagent.persistence.QaHistoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * FeedbackServiceTest 是 JUnit5 + Mockito 的单元测试类，专门测试 FeedbackService 的业务逻辑是否正确。
 * 单元测试特点：不连真实数据库、不启动 Spring 容器，用 mock() 模拟 FeedbackRepository、QaHistoryRepository、
 * ProfileService 这些依赖，只测 FeedbackService 内部业务逻辑。
 */
class FeedbackServiceTest {

    //mock(XXX.class)：创建一个模拟假对象，不是真实实例，不会访问数据库。
    private final FeedbackRepository feedbackRepo = mock(FeedbackRepository.class);
    private final QaHistoryRepository qaRepo = mock(QaHistoryRepository.class);
    private final ProfileService profileService = mock(ProfileService.class);
    private final FeedbackService service =      //直接 new FeedbackService，把三个 mock 对象传给它，脱离 Spring 容器直接运行业务代码。
            new FeedbackService(feedbackRepo, qaRepo, profileService);

    @Test       //正常完整流程，一切参数合法，能查到 term。
    void shouldSaveFeedbackAndUpdateCounter() {
        when(qaRepo.findTermByQaId("qa-1")).thenReturn("郡县制");

        service.record(new FeedbackRequest("qa-1", "TOO_SHALLOW", null));

        verify(feedbackRepo).save("qa-1", "TOO_SHALLOW", null);
        verify(profileService).recordAsk("郡县制");
        verify(profileService).recordFeedbackCounter("郡县制", "too_shallow_count");
    }

    @Test       //根据 qaId 查询不到 term（问答历史丢失）。业务规则：反馈依然要落库，只是跳过 concept_profile 画像更新。
    void shouldStillSaveFeedbackWhenTermNotFound() {
        when(qaRepo.findTermByQaId("qa-x")).thenReturn(null);

        service.record(new FeedbackRequest("qa-x", "JUST_RIGHT", "很好"));

        verify(feedbackRepo).save("qa-x", "JUST_RIGHT", "很好");
        verify(profileService, never()).recordFeedbackCounter(anyString(), anyString());
    }

    @Test   //传入非法 rating，应该抛出参数异常，什么数据库操作都不能执行。
    void shouldRejectIllegalRating() {
        try {
            service.record(new FeedbackRequest("qa-1", "HACK", null));
            throw new AssertionError("应抛异常");
        } catch (IllegalArgumentException expected) {
            // OK
        }
        verifyNoInteractions(feedbackRepo);
    }

    @Test    //qaId 传空字符串，参数校验直接拒绝，不做任何存储。
    void shouldRejectBlankQaId() {
        try {
            service.record(new FeedbackRequest("", "JUST_RIGHT", null));
            throw new AssertionError("应抛异常");
        } catch (IllegalArgumentException expected) {
            // OK
        }
    }


    @Test
    void shouldBoostFamiliarityOnTwoConsecutiveShallow() {
        when(qaRepo.findTermByQaId("qa-1")).thenReturn("郡县制");
        when(feedbackRepo.findRecentRatingsByTerm("郡县制", 2))
                .thenReturn(List.of("TOO_SHALLOW", "TOO_SHALLOW"));

        service.record(new FeedbackRequest("qa-1", "TOO_SHALLOW", null));

        verify(profileService).adjustFamiliarity("郡县制", +1);
    }

    @Test
    void shouldLowerFamiliarityOnTwoConsecutiveDeep() {
        when(qaRepo.findTermByQaId("qa-1")).thenReturn("郡县制");
        when(feedbackRepo.findRecentRatingsByTerm("郡县制", 2))
                .thenReturn(List.of("TOO_DEEP", "TOO_DEEP"));

        service.record(new FeedbackRequest("qa-1", "TOO_DEEP", null));

        verify(profileService).adjustFamiliarity("郡县制", -1);
    }

    @Test
    void shouldNotChangeWhenOnlyOneFeedback() {
        when(qaRepo.findTermByQaId("qa-1")).thenReturn("郡县制");
        when(feedbackRepo.findRecentRatingsByTerm("郡县制", 2))
                .thenReturn(List.of("TOO_SHALLOW"));

        service.record(new FeedbackRequest("qa-1", "TOO_SHALLOW", null));

        verify(profileService, never()).adjustFamiliarity(anyString(), anyInt());
    }

    @Test
    void shouldNotChangeWhenRatingsDiffer() {
        when(qaRepo.findTermByQaId("qa-1")).thenReturn("郡县制");
        when(feedbackRepo.findRecentRatingsByTerm("郡县制", 2))
                .thenReturn(List.of("TOO_SHALLOW", "JUST_RIGHT"));

        service.record(new FeedbackRequest("qa-1", "TOO_SHALLOW", null));

        verify(profileService, never()).adjustFamiliarity(anyString(), anyInt());
    }
}