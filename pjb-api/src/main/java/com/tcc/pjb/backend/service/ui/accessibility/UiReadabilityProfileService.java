package com.tcc.pjb.backend.service.ui.accessibility;

import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiReadabilityProfilePreviewRequestDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiReadabilityProfilePreviewResponseDto;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UiReadabilityProfileService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public UiReadabilityProfilePreviewResponseDto preview(UiReadabilityProfilePreviewRequestDto request) {
        UiReadabilityProfilePreviewRequestDto safe = request == null
                ? new UiReadabilityProfilePreviewRequestDto("", null, false, false, false)
                : request;
        String text = Objects.toString(safe.text(), "").trim();
        int wordCount = text.isBlank() ? 0 : WHITESPACE_PATTERN.split(text.trim()).length;
        int sentenceCount = text.isBlank() ? 0 : text.split("[.!?]+").length;
        int avgSentence = sentenceCount == 0 ? 0 : Math.max(1, wordCount / sentenceCount);
        EnumSet<UiAccessibilityFlag> flags = EnumSet.noneOf(UiAccessibilityFlag.class);
        ArrayList<String> recommendations = new ArrayList<>();

        UiAccessibilityPreset preset = UiAccessibilityPreset.DEFAULT;
        if (safe.lowVision()) {
            preset = UiAccessibilityPreset.HIGH_CONTRAST;
            flags.add(UiAccessibilityFlag.HIGH_CONTRAST);
            flags.add(UiAccessibilityFlag.LARGE_TEXT);
            recommendations.add("ativar contraste alto e texto ampliado");
        }
        if (safe.screenReaderPrimary()) {
            preset = UiAccessibilityPreset.SCREEN_READER_OPTIMIZED;
            flags.add(UiAccessibilityFlag.SCREEN_READER_OPTIMIZED);
            flags.add(UiAccessibilityFlag.KEYBOARD_ONLY);
            recommendations.add("priorizar navegação linear e marcadores semânticos");
        }
        if (safe.cognitiveLoadSensitive() || avgSentence > 22 || wordCount > 350) {
            flags.add(UiAccessibilityFlag.READING_MODE);
            flags.add(UiAccessibilityFlag.REDUCED_MOTION);
            recommendations.add("quebrar blocos longos em segmentos menores");
        }
        String audience = Objects.toString(safe.audience(), "").trim().toLowerCase(Locale.ROOT);
        if (audience.contains("leigo") || audience.contains("cidada") || audience.contains("cidadã") || audience.contains("cidadao") || audience.contains("cidadão")) {
            flags.add(UiAccessibilityFlag.READING_MODE);
            recommendations.add("oferecer também a versão em linguagem simples");
        }
        if (flags.isEmpty()) {
            recommendations.add("perfil padrão suficiente para o conteúdo atual");
        }

        LinkedHashMap<String, Integer> metrics = new LinkedHashMap<>();
        metrics.put("wordCount", wordCount);
        metrics.put("sentenceCount", sentenceCount);
        metrics.put("avgSentenceWords", avgSentence);
        metrics.put("flagMask", (int) UiAccessibilityFlag.maskOf(flags));
        return new UiReadabilityProfilePreviewResponseDto(preset, List.copyOf(flags), Map.copyOf(metrics), List.copyOf(recommendations));
    }
}
