package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDelegatedGovernanceItemResponse(
        String closureId,
        String affiliationId,
        String requestId,
        String organizationScope,
        String destinatarioKind,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        boolean forumOrJudicialUnit,
        boolean delegatedInstitutionalEntry,
        boolean adesaoAptaParaHomologacao,
        boolean duplaChaveSatisfeita,
        boolean afiliacaoHomologada,
        boolean nomeacoesAtivas,
        boolean quatroNiveisFechados,
        boolean orgaoNomeiaEPjbHomologa,
        boolean recertificacaoEmDia,
        boolean diagnosticoEstruturalOk,
        boolean integracaoEndurecida,
        long totalNomeacoes,
        long totalNomeacoesAtivas,
        long totalAdministradores,
        long totalCaixasAtivas,
        List<String> caixasOperacionais,
        List<String> guardRails,
        List<String> missingPillars,
        List<String> fundamentos,
        Instant updatedAt
) {
}
