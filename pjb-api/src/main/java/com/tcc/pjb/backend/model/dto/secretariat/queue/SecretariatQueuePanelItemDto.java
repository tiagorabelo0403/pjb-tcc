package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatQueuePanelItemDto(
    Long workItemId,
    Long processoId,
    String processoNumero,
    String titulo,
    String queueCode,
    String stage,
    String status,
    Integer prioridade,
    Instant referenceAt,
    Instant dueAt,
    String ritoProcessual,
    String classeProcessual,
    String varaLabel,
    String unidadeCodigo,
    String comarca,
    String tribunalCodigo,
    String targetPanelRoute,
    List<String> tags,
    List<SecretariatQueueActionContractDto> actionContracts,
    Map<String, Object> metadata
) {
}
