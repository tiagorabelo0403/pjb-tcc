package com.tcc.pjb.backend.model.dto.processual.movimentacao;

import java.time.Instant;
import java.util.List;

public record MovimentacaoAdjustmentResponse(
        Long adjustmentId,
        String requestUuid,
        Long processoId,
        Long movimentacaoId,
        String modo,
        String status,
        String motivo,
        String descricaoSubstitutiva,
        Integer complianceScore,
        String complianceVerdict,
        List<String> complianceFlags,
        String originalHash,
        String auditHash,
        String ledgerEntryHash,
        Long generatedMovimentacaoId,
        Instant createdAt,
        Instant appliedAt
) {
}
