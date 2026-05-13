package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record ReciboCitacaoHsm(
        UUID protocoloPJB,
        ZonedDateTime instante,
        PjbHardwareSecurityModule.AssinaturaHsm assinaturaHsm,
        String trilhaAuditoria,
        String canalVencedor,
        String documentoAlvo,
        String processoNumero,
        Long processoId,
        String hashPayload,
        boolean mock,
        List<String> canaisTestados,
        List<String> canaisFalhados,
        String recomendacaoProximoPasso
) {
    public ReciboCitacaoHsm {
        if (protocoloPJB == null) {
            throw new IllegalArgumentException("protocoloPJB é obrigatório");
        }
        instante = instante != null ? instante : ZonedDateTime.now();
        trilhaAuditoria = trimToNull(trilhaAuditoria);
        canalVencedor = trimToNull(canalVencedor);
        documentoAlvo = trimToNull(documentoAlvo);
        processoNumero = trimToNull(processoNumero);
        hashPayload = trimToNull(hashPayload);
        canaisTestados = canaisTestados == null ? List.of() : List.copyOf(canaisTestados);
        canaisFalhados = canaisFalhados == null ? List.of() : List.copyOf(canaisFalhados);
        recomendacaoProximoPasso = trimToNull(recomendacaoProximoPasso);
    }

    public UUID protocolo() {
        return protocoloPJB;
    }

    public ZonedDateTime registradoEm() {
        return instante;
    }

    public PjbHardwareSecurityModule.AssinaturaHsm assinatura() {
        return assinaturaHsm;
    }

    public String documentoMascarado() {
        return documentoAlvo;
    }

    public boolean ambienteMock() {
        return mock;
    }

    public String recomendacao() {
        return recomendacaoProximoPasso;
    }

    public boolean foiEntregue() {
        return canalVencedor != null && !canalVencedor.isBlank();
    }

    public boolean exigeFallbackFisico() {
        return !foiEntregue() || canalVencedor.startsWith("FALLBACK");
    }

    public boolean houveFalhaTotal() {
        return exigeFallbackFisico();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
