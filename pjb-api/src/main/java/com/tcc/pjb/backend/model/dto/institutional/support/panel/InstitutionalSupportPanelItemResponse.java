package com.tcc.pjb.backend.model.dto.institutional.support.panel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalSupportPanelItemResponse(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String status,
        String queueCode,
        String inboxKey,
        String ramoDireito,
        String ritoProcessual,
        String classeProcessual,
        String vara,
        String comarca,
        String uf,
        Instant dueAt,
        Instant updatedAt,
        String principalContatoNome,
        String principalContatoEmail,
        Map<String, Object> contactEnvelope,
        List<String> tags,
        Map<String, Object> routes
) {
}
