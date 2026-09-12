package com.tcc.pjb.backend.core.security.accesskey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbProcessAccessKeyPolicyTest {

    private static final Instant AGORA = Instant.parse("2026-09-12T12:00:00Z");

    private final PjbProcessAccessKeyPolicy policy = new PjbProcessAccessKeyPolicy();

    private PjbProcessAccessKeyGrant concessao(Set<PjbProcessAccessKeyScope> escopos,
                                               boolean revogada, boolean sigiloso, long diasDeValidade) {
        return new PjbProcessAccessKeyGrant(
                "0001234-55.2026.8.06.0001", "fingerprint", escopos,
                AGORA.minus(1, ChronoUnit.DAYS), AGORA.plus(diasDeValidade, ChronoUnit.DAYS),
                revogada, sigiloso, "titular");
    }

    @Test
    void concessaoValidaComEscopoConcedidoLiberaAcesso() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.PUBLIC_TIMELINE), false, false, 10),
                PjbProcessAccessKeyScope.PUBLIC_TIMELINE, AGORA);

        assertThat(decisao.allowed()).isTrue();
    }

    @Test
    void chaveRevogadaNegaAcesso() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.PUBLIC_TIMELINE), true, false, 10),
                PjbProcessAccessKeyScope.PUBLIC_TIMELINE, AGORA);

        assertThat(decisao.allowed()).isFalse();
        assertThat(decisao.reasons()).anyMatch(r -> r.contains("revoked"));
    }

    @Test
    void chaveExpiradaNegaAcesso() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.PUBLIC_TIMELINE), false, false, -1),
                PjbProcessAccessKeyScope.PUBLIC_TIMELINE, AGORA);

        assertThat(decisao.allowed()).isFalse();
        assertThat(decisao.reasons()).anyMatch(r -> r.contains("expired"));
    }

    @Test
    void escopoNaoConcedidoNegaAcesso() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.PUBLIC_COVER), false, false, 10),
                PjbProcessAccessKeyScope.FULL_CASE_FILE, AGORA);

        assertThat(decisao.allowed()).isFalse();
        assertThat(decisao.reasons()).anyMatch(r -> r.contains("scope not granted"));
    }

    @Test
    void processoSobSegredoDeJusticaNegaEscopoAmploMesmoConcedido() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.FULL_CASE_FILE), false, true, 10),
                PjbProcessAccessKeyScope.FULL_CASE_FILE, AGORA);

        assertThat(decisao.allowed())
                .as("segredo de justica so admite o canal de resposta a intimacao, ainda que o escopo esteja na chave")
                .isFalse();
        assertThat(decisao.reasons()).anyMatch(r -> r.contains("sealed case"));
    }

    @Test
    void processoSobSegredoDeJusticaAdmiteRespostaAIntimacao() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.RESPONSE_TO_NOTICE), false, true, 10),
                PjbProcessAccessKeyScope.RESPONSE_TO_NOTICE, AGORA);

        assertThat(decisao.allowed()).isTrue();
    }

    @Test
    void validadeAcimaDaJanelaDePoliticaNegaAcesso() {
        var decisao = policy.evaluate(concessao(Set.of(PjbProcessAccessKeyScope.PUBLIC_TIMELINE), false, false, 200),
                PjbProcessAccessKeyScope.PUBLIC_TIMELINE, AGORA);

        assertThat(decisao.allowed()).isFalse();
        assertThat(decisao.reasons()).anyMatch(r -> r.contains("validity exceeds"));
    }

    @Test
    void concessaoAusenteNegaAcesso() {
        var decisao = policy.evaluate(null, PjbProcessAccessKeyScope.PUBLIC_COVER, AGORA);

        assertThat(decisao.allowed()).isFalse();
    }
}
