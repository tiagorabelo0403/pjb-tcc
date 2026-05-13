package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalDelegatedGovernanceItem(
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
    public InstitutionalDelegatedGovernanceItem {
        caixasOperacionais = caixasOperacionais == null ? List.of() : List.copyOf(caixasOperacionais);
        guardRails = guardRails == null ? List.of() : List.copyOf(guardRails);
        missingPillars = missingPillars == null ? List.of() : List.copyOf(missingPillars);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
