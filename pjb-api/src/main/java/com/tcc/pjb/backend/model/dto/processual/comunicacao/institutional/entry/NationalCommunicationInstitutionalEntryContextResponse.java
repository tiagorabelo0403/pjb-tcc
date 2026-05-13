package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.util.List;

public record NationalCommunicationInstitutionalEntryContextResponse(
        String contextId,
        String destinatarioKind,
        String organizacaoKind,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String nucleo,
        String uf,
        String comarca,
        String caixaCodigo,
        String caixaNome,
        String processProfile,
        String funcaoOperacional,
        List<String> capacidades,
        boolean delegacaoAtiva,
        boolean substituicaoAtiva,
        boolean coberturaAtiva,
        boolean plantaoAtivo,
        long totalPendencias,
        long totalSemLeitura,
        long totalUrgentes,
        long totalAtribuidasAoUsuario,
        String landingPanel,
        String landingPath,
        String accentColor,
        int prioridade,
        List<String> fundamentosEntrada
) {}
