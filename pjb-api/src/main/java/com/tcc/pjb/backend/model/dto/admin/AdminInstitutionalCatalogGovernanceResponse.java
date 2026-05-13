package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.Set;

public record AdminInstitutionalCatalogGovernanceResponse(
        String governanceId,
        String unidadeCodigo,
        String destinatarioKind,
        String uf,
        String comarca,
        String foro,
        String ramoDireito,
        String grauJurisdicao,
        String abrangencia,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        boolean ativa,
        boolean suspendeEntregaExterna,
        boolean exigeHomologacaoAdministrativa,
        Set<String> canaisPreferenciais,
        String unidadeSubstitutaCodigo,
        String fundamentoAdministrativo,
        String origem,
        Instant updatedAt
) {
}
