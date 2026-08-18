package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("ocr-status")
public class OcrDegradedHealthIndicator implements HealthIndicator {

    private final DigitalizacaoProperties ocrProps;
    private final MockGuardEnvironmentQuery mockGuardQuery;

    public OcrDegradedHealthIndicator(DigitalizacaoProperties ocrProps,
                                      MockGuardEnvironmentQuery mockGuardQuery) {
        this.ocrProps = Objects.requireNonNull(ocrProps, "ocrProps");
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
    }

    @Override
    public Health health() {
        boolean ocrEnabled = ocrProps.enabled();
        boolean realEnv = mockGuardQuery.isRealEnvironment();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ocrEnabled", ocrEnabled);
        details.put("realEnvironment", realEnv);
        details.put("profile", mockGuardQuery.activeGuardProfile().name());

        if (!ocrEnabled && realEnv) {
            details.put("degradationReason",
                    "pjb.digitalizacao.enabled=false em ambiente real — documentos digitalizados sem extração de texto (OCR_MOCK). Busca processual comprometida até reabilitação.");
            Health.Builder builder = Health.status("DEGRADED");
            details.forEach(builder::withDetail);
            return builder.build();
        }
        Health.Builder builder = Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
