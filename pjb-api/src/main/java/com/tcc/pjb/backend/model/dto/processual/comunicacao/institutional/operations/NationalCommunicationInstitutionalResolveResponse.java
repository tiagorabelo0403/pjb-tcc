package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.util.List;

public record NationalCommunicationInstitutionalResolveResponse(
        String destinatarioKind,
        String papelProcessual,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String caixaNome,
        String canalPrincipal,
        List<String> canaisElegiveis,
        String tribunalCodigo,
        String uf,
        String comarca,
        String foro,
        String ramoDireito,
        String grauJurisdicao,
        String hashResolucao,
        List<String> justificativas,
        String catalogVersion) {
}
