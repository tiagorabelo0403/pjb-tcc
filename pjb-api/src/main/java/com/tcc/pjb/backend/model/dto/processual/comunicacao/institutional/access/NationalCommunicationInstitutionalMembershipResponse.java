package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.util.List;

public record NationalCommunicationInstitutionalMembershipResponse(
        String unidadeCodigo,
        String unidadeNome,
        String destinatarioKind,
        String uf,
        String comarca,
        String caixaCodigo,
        String caixaNome,
        String tipoCaixa,
        String funcaoOperacional,
        String abrangencia,
        List<String> capacidades,
        String justificativa) {
}
