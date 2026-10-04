package com.koodoagent.memory;

import com.koodoagent.dto.ConceptProfileDTO;
import com.koodoagent.persistence.ProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private final ProfileRepository repo = mock(ProfileRepository.class);
    private final ProfileService service = new ProfileService(repo);

    @Test
    void shouldReturnZeroWhenConceptNotFound() {
        when(repo.findByConceptName("郡县制")).thenReturn(Optional.empty());
        assertEquals(0, service.getFamiliarity("郡县制"));
    }

    @Test
    void shouldReturnStoredFamiliarity() {
        ConceptProfileDTO dto = new ConceptProfileDTO(
                1L, "郡县制", 2, 5, 0, 3, 0, "2026-10-01");
        when(repo.findByConceptName("郡县制")).thenReturn(Optional.of(dto));
        assertEquals(2, service.getFamiliarity("郡县制"));
    }

    @Test
    void shouldClampFamiliarityToRange() {
        service.setFamiliarity("X", 5);      // 应裁剪到 3
        verify(repo).updateFamiliarity("X", 3);

        service.setFamiliarity("Y", -2);     // 应裁剪到 0
        verify(repo).updateFamiliarity("Y", 0);
    }

    @Test
    void adjustShouldUseCurrentValue() {
        when(repo.findByConceptName("X"))
                .thenReturn(Optional.of(new ConceptProfileDTO(
                        1L, "X", 1, 0, 0, 0, 0, null)));

        service.adjustFamiliarity("X", 1);
        verify(repo).updateFamiliarity("X", 2);

        service.adjustFamiliarity("X", -3);
        verify(repo).updateFamiliarity("X", 0);
    }

    @Test
    void recordAskShouldNotTouchFamiliarity() {
        service.recordAsk("郡县制");
        verify(repo).upsertOnAsk("郡县制");
        verify(repo, never()).updateFamiliarity(anyString(), anyInt());
    }

    @Test
    void shouldIgnoreBlankConcept() {
        service.recordAsk("   ");
        service.setFamiliarity(null, 2);
        verifyNoInteractions(repo);
    }
}