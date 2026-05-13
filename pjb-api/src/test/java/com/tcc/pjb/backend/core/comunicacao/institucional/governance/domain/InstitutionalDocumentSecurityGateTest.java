package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalDocumentSecurityGateTest {

    @Test
    void asMapShouldExposeManualApprovalAndGeneratedAtWithoutNulls() {
        Instant generatedAt = Instant.parse("2026-04-09T12:00:00Z");
        InstitutionalDocumentSecurityGate gate = new InstitutionalDocumentSecurityGate(
                "OFICIAL_OFICIO",
                "aff-1",
                "nom-1",
                "UNI-1",
                "CAIXA-1",
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                List.of("CERTIFICADO_QUALIFICADO"),
                List.of("PORTARIA_INTERNA"),
                generatedAt
        );

        Map<String, Object> payload = gate.asMap();

        assertThat(payload)
                .containsEntry("operationCode", "OFICIAL_OFICIO")
                .containsEntry("manualApproval", true)
                .containsEntry("generatedAt", generatedAt)
                .containsEntry("findings", List.of("CERTIFICADO_QUALIFICADO"));
        assertThat(payload).doesNotContainValue(null);
    }
}
