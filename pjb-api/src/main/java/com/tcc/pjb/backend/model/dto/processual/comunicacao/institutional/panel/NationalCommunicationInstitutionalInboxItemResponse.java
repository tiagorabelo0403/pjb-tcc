package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalInboxItemResponse(
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String unidadeSigla,
        String caixaCodigo,
        String destinatarioKind,
        String papelProcessual,
        String tipoComunicacao,
        String status,
        String gateCode,
        boolean bloqueiaFluxo,
        String horizontalDataPlaneKey,
        String rlsScopeKey,
        String coverageMode,
        boolean readOnly,
        boolean requiresStepUp,
        boolean requiresQualifiedCertificate,
        Instant disponibilizadaEm,
        Instant recebidaEm,
        Instant cientificadaEm,
        Instant cumpridaEm,
        Instant prazoCienciaEm,
        Instant prazoRespostaEm,
        Instant updatedAt,
        List<String> justificativas,
        String hashIntegridade
) {
    public NationalCommunicationInstitutionalInboxItemResponse(
            String inboxItemId,
            String expedicaoUuid,
            Long processoId,
            String processoNumero,
            String unidadeCodigo,
            String unidadeSigla,
            String destinatarioKind,
            String papelProcessual,
            String tipoComunicacao,
            String caixaCodigoOrigem,
            String caixaCodigo,
            String canalPrincipal,
            String status,
            String gateCode,
            boolean bloqueiaFluxo,
            Long atribuidoUsuarioId,
            Long ultimoOperadorUsuarioId,
            String horizontalDataPlaneKey,
            String rlsScopeKey,
            String coverageMode,
            boolean readOnly,
            boolean requiresStepUp,
            boolean requiresQualifiedCertificate,
            java.time.Instant disponibilizadaEm,
            java.time.Instant recebidaEm,
            java.time.Instant cientificadaEm,
            java.time.Instant cumpridaEm,
            java.time.Instant prazoCienciaEm,
            java.time.Instant prazoRespostaEm,
            java.time.Instant updatedAt,
            java.util.List<String> justificativas) {
        this(expedicaoUuid, processoId, processoNumero, unidadeCodigo, unidadeSigla, caixaCodigo, destinatarioKind, papelProcessual, tipoComunicacao, status, gateCode, bloqueiaFluxo, horizontalDataPlaneKey, rlsScopeKey, coverageMode, readOnly, requiresStepUp, requiresQualifiedCertificate, disponibilizadaEm, recebidaEm, cientificadaEm, cumpridaEm, prazoCienciaEm, prazoRespostaEm, updatedAt, justificativas, inboxItemId);
    }

    public NationalCommunicationInstitutionalInboxItemResponse(
            String expedicaoUuid,
            Long processoId,
            String processoNumero,
            String destinatarioKind,
            String papelProcessual,
            String tipoComunicacao,
            String status,
            String caixaCodigo,
            String unidadeCodigo,
            String gateCode,
            String coverageMode,
            boolean readOnly,
            java.time.Instant recebidaEm,
            java.time.Instant cientificadaEm,
            java.time.Instant cumpridaEm,
            java.time.Instant prazoRespostaEm,
            String hashIntegridade,
            java.util.List<String> justificativas,
            java.util.List<String> capacidades) {
        this(expedicaoUuid, processoId, processoNumero, unidadeCodigo, null, caixaCodigo, destinatarioKind, papelProcessual, tipoComunicacao, status, gateCode, false, null, null, coverageMode, readOnly, false, false, null, recebidaEm, cientificadaEm, cumpridaEm, null, prazoRespostaEm, null, merge(justificativas, capacidades), hashIntegridade);
    }

    private static java.util.List<String> merge(java.util.List<String> justificativas, java.util.List<String> capacidades) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (justificativas != null) {
            out.addAll(justificativas);
        }
        if (capacidades != null) {
            capacidades.stream().filter(item -> item != null && !item.isBlank()).map(item -> "CAPACIDADE=" + item.trim()).forEach(out::add);
        }
        return java.util.List.copyOf(out);
    }
}
