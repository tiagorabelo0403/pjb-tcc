package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;




public record RitoRuleDraftItemDto(
        String ritoResolved,
        String ritoChosen,
        long occurrences,
        List<String> sampleReasons,
        List<String> draftRules
) {
}
