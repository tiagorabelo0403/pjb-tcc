package com.tcc.pjb.backend.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class GovBrAssurancePolicyTest {

    private final GovBrAssuranceExtractor extractor = new GovBrAssuranceExtractor();
    private final GovBrAssurancePolicy policy = new GovBrAssurancePolicy();

    @Test
    void shouldExtractOuroFromAcr() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("acr", "loa3")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("123")
                .build();
        String nivel = extractor.extract(new JwtAuthenticationToken(jwt, List.of()));
        assertThat(nivel).isEqualTo("ouro");
        assertThat(policy.atoNormatizadoAtendido(nivel, true)).isTrue();
        assertThat(policy.exigeStepUp(nivel, true)).isFalse();
    }

    @Test
    void shouldRequireStepUpWhenBronzeForSensitiveAct() {
        assertThat(policy.atoNormatizadoAtendido("bronze", true)).isFalse();
        assertThat(policy.exigeStepUp("bronze", true)).isTrue();
        assertThat(policy.atoNormatizadoAtendido("prata", false)).isTrue();
    }

    @Test
    void shouldMarkInfanciaAsSensitiveCitizenAct() {
        Processo processo = Processo.builder()
                .ramoDireito(RamoDireito.INFANCIA_JUVENTUDE)
                .classeProcessual("guarda de menor")
                .build();

        assertThat(policy.isSensitiveCitizenAct(processo, null)).isTrue();
    }

    @Test
    void shouldMarkHighValueSettlementAsSensitiveCitizenAct() {
        Processo processo = Processo.builder()
                .valorCausa(new BigDecimal("85000"))
                .janelaAcordoResumo("pedido de homologação de acordo")
                .build();

        assertThat(policy.isSensitiveCitizenAct(processo, "acordo judicial com pagamento parcelado")).isTrue();
    }

    @Test
    void shouldMarkTreasuryExecutionAsSensitiveCitizenAct() {
        Processo processo = Processo.builder()
                .assunto("execução contra fazenda pública")
                .build();

        assertThat(policy.isSensitiveCitizenAct(processo, null)).isTrue();
    }

    @Test
    void shouldIgnoreOrdinaryCitizenActWithoutSensitiveMarkers() {
        Processo processo = Processo.builder()
                .assunto("obrigação de fazer em saúde suplementar")
                .valorCausa(new BigDecimal("1200"))
                .build();

        assertThat(policy.isSensitiveCitizenAct(processo, "petição de juntada simples")).isFalse();
    }
}
