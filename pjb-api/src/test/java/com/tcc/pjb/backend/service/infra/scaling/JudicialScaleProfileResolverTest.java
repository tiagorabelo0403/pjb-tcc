package com.tcc.pjb.backend.service.infra.scaling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JudicialScaleProfileResolverTest {

    private final JudicialScaleProfileResolver resolver = new JudicialScaleProfileResolver();

    @Test
    void shouldResolveTurmaRecursalFromRequestHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/secretaria/fila");
        request.addHeader(JudicialScaleProfileResolver.HEADER_INSTANCIA, "TURMA_RECURSAL");
        request.addHeader(JudicialScaleProfileResolver.HEADER_RAMO, "JUIZADO_ESPECIAL");

        JudicialScaleProfileResolver.JudicialScalePolicy policy = resolver.resolvePolicy(request);

        assertThat(policy.profile()).isEqualTo(JudicialScaleProfile.TURMA_RECURSAL);
        assertThat(policy.branchClass()).isEqualTo("JUIZADO_ESPECIAL");
        assertThat(policy.cacheHotPreferred()).isTrue();
    }

    @Test
    void shouldResolveTribunalSuperiorFromInboxKeyAndJobType() {
        JudicialScaleProfileResolver.JudicialScalePolicy policy = resolver.resolvePolicyFromInbox(
                "SEC:STJ:TRIBUNAL_SUPERIOR:GABINETE:DF:brasilia:secao",
                "PUBLICACAO_ACORDAO_SUPERIOR"
        );

        assertThat(policy.profile()).isEqualTo(JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR);
        assertThat(policy.instanceClass()).isEqualTo("INSTANCIA_SUPERIOR");
    }
}
