package com.tcc.pjb.backend.financial.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FinancialAiLegacyResponseMapperTest {

    @Test
    void shouldNormalizeWarningsAndStripNestedEnvelope() {
        IARequest request = IARequest.builder()
                .origem("RADAR")
                .acao("ANALISAR_VALOR_CAUSA")
                .build();

        IAResponse raw = IAResponse.builder()
                .origem("FINANCEIRA_V2")
                .status(IAResponse.StatusIA.ALERTA)
                .confianca(0.67d)
                .texto("Necessario revisar o valor da causa")
                .alertasCriticos(List.of("A", "A", "B"))
                .metadados(Map.of(
                        "financial_ai", Map.of("legacy", true),
                        "provisao_range", Map.of("min", 100, "max", 200)
                ))
                .dataGeracao(Instant.parse("2026-03-20T13:00:00Z"))
                .build();

        FinancialAiDescriptor descriptor = new FinancialAiDescriptor(
                "FINANCIAL_AI",
                ApiVersion.V2,
                "Resumo",
                java.util.Set.of("risk"),
                Instant.parse("2026-03-20T12:00:00Z")
        );

        FinancialAiResponse unified = FinancialAiLegacyResponseMapper.toUnified(request, raw, ApiVersion.V2, descriptor);

        assertThat(unified.warnings()).containsExactly("A", "B");
        assertThat(unified.outputs()).doesNotContainKey("financial_ai");
        assertThat(unified.outputMap("provisao_range")).containsEntry("max", 200);
    }
}
