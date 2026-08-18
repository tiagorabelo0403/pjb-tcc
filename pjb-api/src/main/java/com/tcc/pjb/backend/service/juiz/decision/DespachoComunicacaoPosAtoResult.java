package com.tcc.pjb.backend.service.juiz.decision;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DespachoComunicacaoPosAtoResult(
        Long djePublicacaoId,
        String djeStatus,
        UUID djeOutboxEventId,
        int intimacoesCriadas,
        int prazosCriados,
        List<Long> cienciaIds,
        List<UUID> outboxEventIds,
        LocalDate prazoComecaEm,
        LocalDate prazoFinalEstimado,
        boolean falhaControlada
) {

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> dados = new LinkedHashMap<>();
        dados.put("djePublicacaoId", djePublicacaoId);
        dados.put("djeStatus", djeStatus);
        dados.put("djeOutboxEventId", djeOutboxEventId == null ? "" : djeOutboxEventId.toString());
        dados.put("intimacoesCriadas", intimacoesCriadas);
        dados.put("prazosCriados", prazosCriados);
        dados.put("cienciaIds", cienciaIds);
        dados.put("outboxEventIds", outboxEventIds.stream().map(UUID::toString).toList());
        dados.put("prazoComecaEm", prazoComecaEm);
        dados.put("prazoFinalEstimado", prazoFinalEstimado);
        dados.put("falhaControlada", falhaControlada);
        return Collections.unmodifiableMap(dados);
    }
}
