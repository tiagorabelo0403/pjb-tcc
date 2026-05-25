package com.tcc.pjb.backend.core.observability.systemhealth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("pjbCriticalIntegrationReadiness")
public class PjbCriticalIntegrationReadinessHealthIndicator implements HealthIndicator {

    private final boolean bnmpEnabled;
    private final String bnmpToken;
    private final boolean djeEnabled;
    private final boolean financialEnabled;
    private final boolean financialDryRun;
    private final String sisbajudBaseUrl;
    private final String renajudBaseUrl;
    private final String infojudBaseUrl;

    public PjbCriticalIntegrationReadinessHealthIndicator(
            @Value("${pjb.bnmp.enabled:false}") boolean bnmpEnabled,
            @Value("${pjb.bnmp.token:}") String bnmpToken,
            @Value("${pjb.dje.enabled:false}") boolean djeEnabled,
            @Value("${pjb.integracoes.judicial-financeiro.enabled:false}") boolean financialEnabled,
            @Value("${pjb.integracoes.judicial-financeiro.dry-run:true}") boolean financialDryRun,
            @Value("${pjb.integrations.sisbajud.base-url:}") String sisbajudBaseUrl,
            @Value("${pjb.integrations.renajud.base-url:}") String renajudBaseUrl,
            @Value("${pjb.integrations.infojud.base-url:}") String infojudBaseUrl) {
        this.bnmpEnabled = bnmpEnabled;
        this.bnmpToken = bnmpToken;
        this.djeEnabled = djeEnabled;
        this.financialEnabled = financialEnabled;
        this.financialDryRun = financialDryRun;
        this.sisbajudBaseUrl = sisbajudBaseUrl;
        this.renajudBaseUrl = renajudBaseUrl;
        this.infojudBaseUrl = infojudBaseUrl;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean outOfService = false;

        details.put("bnmpEnabled", bnmpEnabled);
        if (bnmpEnabled) {
            boolean tokenMissing = bnmpToken == null || bnmpToken.isBlank();
            details.put("bnmpTokenConfigured", !tokenMissing);
            if (tokenMissing) {
                details.put("bnmpStatus", "OUT_OF_SERVICE: habilitado sem token configurado");
                outOfService = true;
            } else {
                details.put("bnmpStatus", "configured");
            }
        } else {
            details.put("bnmpStatus", "disabled");
        }

        details.put("djeEnabled", djeEnabled);
        details.put("djeStatus", djeEnabled ? "enabled" : "disabled");

        details.put("judicialFinanceiroEnabled", financialEnabled);
        details.put("judicialFinanceiroDryRun", financialDryRun);
        if (financialEnabled && !financialDryRun) {
            boolean sisbajudMissing = sisbajudBaseUrl == null || sisbajudBaseUrl.isBlank();
            boolean renajudMissing = renajudBaseUrl == null || renajudBaseUrl.isBlank();
            boolean infojudMissing = infojudBaseUrl == null || infojudBaseUrl.isBlank();
            details.put("sisbajudBaseUrlConfigured", !sisbajudMissing);
            details.put("renajudBaseUrlConfigured", !renajudMissing);
            details.put("infojudBaseUrlConfigured", !infojudMissing);
            if (sisbajudMissing || renajudMissing || infojudMissing) {
                details.put("judicialFinanceiroStatus", "OUT_OF_SERVICE: habilitado com dry-run=false mas base-url nao configurada");
                outOfService = true;
            } else {
                details.put("judicialFinanceiroStatus", "configured");
            }
        } else if (financialEnabled) {
            details.put("judicialFinanceiroStatus", "enabled-dry-run");
        } else {
            details.put("judicialFinanceiroStatus", "disabled");
        }

        Health.Builder builder = outOfService ? Health.status("OUT_OF_SERVICE") : Health.up();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
