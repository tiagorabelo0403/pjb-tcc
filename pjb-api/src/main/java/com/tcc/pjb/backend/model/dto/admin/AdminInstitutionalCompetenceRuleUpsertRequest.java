package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminInstitutionalCompetenceRuleUpsertRequest(
        @NotBlank String ruleId,
        @NotNull DestinatarioInstitucionalKind destinatarioKind,
        @NotNull PapelProcessualInstitucional papelProcessual,
        String uf,
        String comarca,
        String foro,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        @NotBlank String unidadeCodigo,
        Integer prioridade,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        Boolean ativa,
        String origem,
        String fundamentoAdministrativo
) {
}
