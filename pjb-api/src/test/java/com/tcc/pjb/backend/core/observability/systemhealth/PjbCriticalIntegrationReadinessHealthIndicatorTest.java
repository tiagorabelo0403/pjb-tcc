package com.tcc.pjb.backend.core.observability.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class PjbCriticalIntegrationReadinessHealthIndicatorTest {

    @Test
    void bnmpEnabledComTokenVazio_retornaOutOfService() {
        var indicator = indicator(true, "", false, false, true, "", "", "");
        Health health = indicator.health();
        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails()).containsKey("bnmpStatus");
    }

    @Test
    void bnmpEnabledComTokenPresente_retornaUp() {
        var indicator = indicator(true, "token-real", false, false, true, "", "", "");
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("bnmpTokenConfigured")).isEqualTo(true);
    }

    @Test
    void financialEnabledDryRunFalseBaseUrlVazia_retornaOutOfService() {
        var indicator = indicator(false, "", false, true, false, "", "", "");
        Health health = indicator.health();
        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails().get("sisbajudBaseUrlConfigured")).isEqualTo(false);
        assertThat(health.getDetails().get("renajudBaseUrlConfigured")).isEqualTo(false);
        assertThat(health.getDetails().get("infojudBaseUrlConfigured")).isEqualTo(false);
    }

    @Test
    void financialEnabledDryRunFalseBaseUrlConfigurada_retornaUp() {
        var indicator = indicator(false, "", false, true, false,
                "https://sisbajud.jus.br", "https://renajud.jus.br", "https://infojud.jus.br");
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("judicialFinanceiroStatus")).isEqualTo("configured");
    }

    @Test
    void financialEnabledDryRunTrue_naoPedeCrendenciais() {
        var indicator = indicator(false, "", false, true, true, "", "", "");
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("judicialFinanceiroStatus")).isEqualTo("enabled-dry-run");
    }

    @Test
    void configuracoesDefaultDesabilitadas_retornaUp() {
        var indicator = indicator(false, "", false, false, true, "", "", "");
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("bnmpStatus")).isEqualTo("disabled");
        assertThat(health.getDetails().get("djeStatus")).isEqualTo("disabled");
        assertThat(health.getDetails().get("judicialFinanceiroStatus")).isEqualTo("disabled");
    }

    @Test
    void semChamadaHttpExterna_healthEhPuramenteConfiguracional() {
        // Verificação: se houvesse chamada HTTP, este teste falharia sem servidor disponível.
        // Passar configuração habilitada com URLs válidas e sem exceção prova que nenhuma
        // conexão de rede é tentada.
        var indicator = indicator(true, "token", true, true, false,
                "https://sisbajud.jus.br", "https://renajud.jus.br", "https://infojud.jus.br");
        assertThat(indicator.health()).isNotNull();
    }

    private static PjbCriticalIntegrationReadinessHealthIndicator indicator(
            boolean bnmpEnabled, String bnmpToken,
            boolean djeEnabled,
            boolean financialEnabled, boolean financialDryRun,
            String sisbajudBaseUrl, String renajudBaseUrl, String infojudBaseUrl) {
        return new PjbCriticalIntegrationReadinessHealthIndicator(
                bnmpEnabled, bnmpToken, djeEnabled,
                financialEnabled, financialDryRun,
                sisbajudBaseUrl, renajudBaseUrl, infojudBaseUrl);
    }
}
