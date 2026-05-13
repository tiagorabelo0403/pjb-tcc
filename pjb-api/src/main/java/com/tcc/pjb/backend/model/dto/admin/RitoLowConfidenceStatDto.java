package com.tcc.pjb.backend.model.dto.admin;




public record RitoLowConfidenceStatDto(
        String ritoResolved,
        long count,
        double avgConfidence
) {
}
