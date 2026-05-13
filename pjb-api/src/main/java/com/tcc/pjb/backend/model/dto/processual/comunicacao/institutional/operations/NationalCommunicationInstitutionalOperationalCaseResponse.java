package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOperationalCaseResponse(
        String scenarioCode,
        String scenarioName,
        String destinatarioKind,
        String orgaoSigla,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String caixaNome,
        boolean recebimentoPermitido,
        boolean triagemPermitida,
        boolean minutaPermitida,
        boolean assinaturaOuManifestacaoPermitida,
        boolean peticionamentoPermitido,
        boolean confirmacaoCustodiaPermitida,
        boolean registroCumprimentoPermitido,
        boolean titularObrigatorio,
        String landingPanel,
        List<String> fundamentos,
        Instant generatedAt
) {
}
