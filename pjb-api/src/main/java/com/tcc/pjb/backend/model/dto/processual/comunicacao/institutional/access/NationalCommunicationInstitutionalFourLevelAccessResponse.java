package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record NationalCommunicationInstitutionalFourLevelAccessResponse(
        Long userId,
        String userName,
        String tipoUsuario,
        String identityBaseCode,
        String affiliationId,
        String destinatarioKind,
        String organizationScope,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String caixaNome,
        String nominationRole,
        String funcaoOperacional,
        String processProfile,
        Set<String> capacidades,
        String landingPanel,
        boolean cadastroInstitucionalResolvido,
        boolean estruturaInternaResolvida,
        boolean pessoaVinculada,
        boolean contextoOperacionalAtivo,
        boolean plantaoAtivo,
        boolean substituicaoAtiva,
        boolean delegacaoAtiva,
        boolean autorizado,
        List<String> fundamentos,
        Instant generatedAt
) {
}
