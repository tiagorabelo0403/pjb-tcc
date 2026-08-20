package com.tcc.pjb.backend.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CapabilityRateLimiterAnonymousSubjectTest {

    private CapabilityRateLimiter limiter() {
        CapabilityRateLimitProperties props = new CapabilityRateLimitProperties();
        props.setEnabled(true);
        // ApiVersion.latest() custa versionCost.v3 (5 tokens) por chamada — o orcamento
        // precisa ser >= o custo de uma chamada para a primeira nao ser negada de saida.
        props.setDefaultLimitTokens(5);
        props.setWindowSeconds(60);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        return new CapabilityRateLimiter(props, new LocalSlidingWindowRateLimitStore(), clock);
    }

    @Test
    void ipsAnonimosDiferentesNaoCompartilhamOMesmoOrcamento() {
        CapabilityRateLimiter limiter = limiter();

        CapabilityRateLimitDecision ip1PrimeiraChamada = limiter.evaluate(
                CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), "9.9.9.1");
        CapabilityRateLimitDecision ip1SegundaChamadaEstourou = limiter.evaluate(
                CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), "9.9.9.1");
        CapabilityRateLimitDecision ip2PrimeiraChamadaAindaLivre = limiter.evaluate(
                CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), "9.9.9.2");

        assertThat(ip1PrimeiraChamada.allowed()).isTrue();
        assertThat(ip1SegundaChamadaEstourou.allowed()).isFalse();
        assertThat(ip2PrimeiraChamadaAindaLivre.allowed()).isTrue();
    }

    @Test
    void mesmoIpAnonimoCompartilhaOOrcamentoEntreChamadas() {
        CapabilityRateLimiter limiter = limiter();

        limiter.evaluate(CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), "9.9.9.5");
        CapabilityRateLimitDecision segundaChamadaMesmoIp = limiter.evaluate(
                CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), "9.9.9.5");

        assertThat(segundaChamadaMesmoIp.allowed()).isFalse();
    }

    @Test
    void semFallbackDeIpMantemComportamentoAnonimoUnicoAntigo() {
        CapabilityRateLimiter limiter = limiter();

        limiter.evaluate(CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest());
        CapabilityRateLimitDecision segundaChamadaSemIp = limiter.evaluate(
                CapabilityRateLimitDomain.CITIZEN, null, "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest());

        assertThat(segundaChamadaSemIp.allowed()).isFalse();
    }
}
