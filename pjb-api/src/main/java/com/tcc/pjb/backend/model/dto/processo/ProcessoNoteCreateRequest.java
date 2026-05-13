package com.tcc.pjb.backend.model.dto.processo;

import java.util.List;

public record ProcessoNoteCreateRequest(
    String body,
    List<String> tags
) {
}
