package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;
import java.util.Map;

public record SecretariatQueuePanelSnapshotDto(
    String inboxKey,
    String inboxDescriptor,
    String deskDescriptor,
    String dashboardBucket,
    List<SecretariatQueuePanelGroupDto> byProcesso,
    List<SecretariatQueuePanelGroupDto> byRito,
    List<SecretariatQueuePanelGroupDto> byVara,
    List<SecretariatQueuePanelGroupDto> byData,
    Map<String, Object> metadata,
    List<SecretariatQueuePanelItemDto> minhasTarefas,
    List<SecretariatQueuePanelItemDto> tarefasDaUnidade,
    List<SecretariatQueuePanelItemDto> assinaturasPendentes,
    List<SecretariatQueuePanelItemDto> expedientesPendentes,
    Map<String, Long> agrupadores,
    List<String> riscosOperacionais,
    List<String> insights
) {
}
