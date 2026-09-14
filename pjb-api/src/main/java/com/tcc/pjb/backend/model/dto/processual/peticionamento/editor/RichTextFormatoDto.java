package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import java.util.List;

public record RichTextFormatoDto(
        String model,
        String enforcement,
        List<String> marks,
        List<String> blocks,
        List<Integer> headingLevels,
        List<String> textAlign,
        List<String> fonts,
        List<String> fontSizes,
        List<String> urlSchemes
) {
}
