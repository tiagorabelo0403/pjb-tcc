package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.time.Instant;
import java.util.List;

public record InstitutionalOperationalCaseSummary(
        String scenarioCode,
        String scenarioName,
        DestinatarioInstitucionalKind destinatarioKind,
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
        InstitutionalEntryLandingPanel landingPanel,
        List<String> fundamentos,
        Instant generatedAt
) {
}
