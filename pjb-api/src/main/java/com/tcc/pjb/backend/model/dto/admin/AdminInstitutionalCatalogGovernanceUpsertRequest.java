package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminInstitutionalCatalogGovernanceUpsertRequest(
        @NotBlank String governanceId,
        @NotBlank String unidadeCodigo,
        @NotNull DestinatarioInstitucionalKind destinatarioKind,
        String uf,
        String comarca,
        String foro,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        @NotNull AbrangenciaGovernancaInstitucional abrangencia,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        Boolean ativa,
        Boolean suspendeEntregaExterna,
        Boolean exigeHomologacaoAdministrativa,
        Set<CanalComunicacaoInstitucional> canaisPreferenciais,
        String unidadeSubstitutaCodigo,
        String fundamentoAdministrativo,
        String origem
) {
}
