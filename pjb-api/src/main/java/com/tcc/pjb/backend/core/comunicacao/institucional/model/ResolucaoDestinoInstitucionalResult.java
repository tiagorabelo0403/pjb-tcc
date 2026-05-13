package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;

public record ResolucaoDestinoInstitucionalResult(
        AlvoInstitucional alvo,
        StatusComunicacaoInstitucional status,
        List<String> justificativas,
        String catalogVersion
) {
    public ResolucaoDestinoInstitucionalResult {
        if (alvo == null) {
            throw new IllegalArgumentException("alvo é obrigatório");
        }
        if (status == null) {
            throw new IllegalArgumentException("status é obrigatório");
        }
        justificativas = PayloadMaps.copyDistinctStrings(justificativas);
        if (catalogVersion == null || catalogVersion.isBlank()) {
            throw new IllegalArgumentException("catalogVersion é obrigatório");
        }
        catalogVersion = catalogVersion.trim();
    }
}
