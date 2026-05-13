package com.tcc.pjb.backend.financial.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinancialAiResponseFactoryTest {

    @Test
    void shouldCreateUnifiedEnvelopeForV3() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC);
        FinancialAiResponseFactory factory = new FinancialAiResponseFactory(clock);

        IARequest request = IARequest.builder()
                .origem("BATNA")
                .acao("ESTIMATIVA_LITIGIO")
                .payload("valorCausa", new BigDecimal("1000.00"))
                .build();

        IAResponse raw = IAResponse.builder()
                .origem("FINANCEIRA_V3")
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(0.91d)
                .texto("Analise pronta")
                .metadados(Map.of(
                        "custas_range", Map.of("min", new BigDecimal("10.00"), "max", new BigDecimal("20.00")),
                        "risco_sucumbencia", 0.15d
                ))
                .dataGeracao(Instant.parse("2026-03-20T12:05:00Z"))
                .build();

        FinancialAiResponse unified = factory.from(request, raw, ApiVersion.V3);
        Map<String, Object> envelope = factory.envelope(request, raw, ApiVersion.V3);

        assertThat(unified.version()).isEqualTo(ApiVersion.V3);
        assertThat(unified.status()).isEqualTo(FinancialAiStatus.SUCCESS);
        assertThat(unified.outputMap("custas_range")).containsEntry("min", new BigDecimal("10.00"));
        assertThat(unified.capabilities()).contains("explain");
        assertThat(envelope).containsKeys("financial_ai", "financial_ai_descriptor");
    }
}
