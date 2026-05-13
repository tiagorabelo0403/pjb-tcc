package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;

public record AdminInstitutionalCompetenceRuleResponse(
        String ruleId,
        String destinatarioKind,
        String papelProcessual,
        String uf,
        String comarca,
        String foro,
        String ramoDireito,
        String grauJurisdicao,
        String unidadeCodigo,
        int prioridade,
        Instant vigenciaInicio,
        Instant vigenciaFim,
        boolean ativa,
        String origem,
        String fundamentoAdministrativo,
        Instant updatedAt
) {
}
