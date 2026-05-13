package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OficialJusticaCumprimentoEncerramentoResponse(
        String status,
        Instant generatedAt,
        Long processoId,
        String processoNumero,
        Long workItemId,
        String mandadoReference,
        String outcome,
        boolean cienciaObrigatoria,
        boolean cienciaConfirmada,
        boolean oficioOriginalObrigatorio,
        boolean oficioOriginalEmitido,
        ChecklistSummary checklist,
        OperationalBundle bundle,
        Map<String, Object> cienciaProcessual,
        Map<String, Object> oficioOriginalDireto,
        Map<String, Object> mandadoFormalAssinado,
        Map<String, Object> certidaoFormalAssinada,
        Map<String, Object> autoCumprimentoAssinado,
        Map<String, Object> recebimentoSecretaria,
        Map<String, Object> processoContexto,
        Map<String, Object> oficialResponsavel,
        List<String> alerts
) {
    public OficialJusticaCumprimentoEncerramentoResponse {
        cienciaProcessual = immutableMap(cienciaProcessual);
        oficioOriginalDireto = immutableMap(oficioOriginalDireto);
        mandadoFormalAssinado = immutableMap(mandadoFormalAssinado);
        certidaoFormalAssinada = immutableMap(certidaoFormalAssinada);
        autoCumprimentoAssinado = immutableMap(autoCumprimentoAssinado);
        recebimentoSecretaria = immutableMap(recebimentoSecretaria);
        processoContexto = immutableMap(processoContexto);
        oficialResponsavel = immutableMap(oficialResponsavel);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record ChecklistSummary(
            String tipoDiligencia,
            boolean mandadoConferido,
            boolean alvoConfirmado,
            boolean enderecoConferido,
            boolean resultadoRegistrado,
            boolean cadeiaCustodiaConferida,
            boolean certidaoGerada,
            boolean formalizacaoGerada,
            boolean juntadaGerada,
            boolean cienciaConfirmada,
            boolean oficioOriginalDiretoConcluido,
            String digestSha256
    ) {
    }

    public record OperationalBundle(
            Long encerramentoId,
            Long certidaoId,
            Long formalizacaoId,
            UUID minutaDocumentoId,
            Long juntadaId,
            UUID pacoteDocumentoId,
            String bundleReference,
            String bundleDigestSha256,
            String workItemStatusFinal
    ) {
    }

    private static Map<String, Object> immutableMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}
