package com.tcc.pjb.backend.model.dto.processual.comunicacao.routing;

import java.util.List;

public record NationalCommunicationProcessualRecipientResolveResponse(
        String destinatarioProcessualKind,
        String trilhoComunicacao,
        String legacyKind,
        String documentoPrincipal,
        String nomeExibicao,
        String destinatarioInstitucionalKind,
        String papelProcessualInstitucional,
        String unidadeInstitucionalCodigo,
        boolean usaFluxoPessoal,
        boolean usaFluxoInstitucional,
        boolean exigeCaixaInstitucional,
        boolean exigeIntimacaoPessoal,
        boolean admiteCitacao,
        boolean admiteIntimacao,
        String hashResolucao,
        List<String> justificativas) {
}
