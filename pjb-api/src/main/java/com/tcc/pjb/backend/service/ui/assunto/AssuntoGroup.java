package com.tcc.pjb.backend.service.ui.assunto;

import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;

public record AssuntoGroup(
    String id,
    Map<UiTheme, String> colors,
    String icon,
    String pattern,
    Map<UiPersona, String> labels,
    Map<UiPersona, String> descriptions,
    List<String> matchAny
) {
}
