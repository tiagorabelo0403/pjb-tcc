package com.tcc.pjb.backend.service.ui.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiReadabilityProfilePreviewRequestDto;
import org.junit.jupiter.api.Test;

class UiReadabilityProfileServiceTest {

    private final UiReadabilityProfileService service = new UiReadabilityProfileService();

    @Test
    void shouldRecommendScreenReaderProfileWhenRequested() {
        var response = service.preview(new UiReadabilityProfilePreviewRequestDto(
                "Texto simples de teste para leitura guiada.",
                "cidadão",
                false,
                true,
                true
        ));

        assertThat(response.preset()).isEqualTo(UiAccessibilityPreset.SCREEN_READER_OPTIMIZED);
        assertThat(response.recommendedFlags()).contains(UiAccessibilityFlag.SCREEN_READER_OPTIMIZED, UiAccessibilityFlag.KEYBOARD_ONLY, UiAccessibilityFlag.READING_MODE);
    }

    @Test
    void shouldRecommendHighContrastForLowVision() {
        var response = service.preview(new UiReadabilityProfilePreviewRequestDto(
                "Texto de teste para baixa visão.",
                "advogado",
                true,
                false,
                false
        ));

        assertThat(response.preset()).isEqualTo(UiAccessibilityPreset.HIGH_CONTRAST);
        assertThat(response.recommendedFlags()).contains(UiAccessibilityFlag.HIGH_CONTRAST, UiAccessibilityFlag.LARGE_TEXT);
    }
}
