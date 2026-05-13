package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbProcessoSigiloRlsFilterTest {

    @Test
    void deveInjetarHintsDeRlsNosFluxosSigilososDoProcesso() throws Exception {
        ProcessoSigiloRlsEnvelopeService service = mock(ProcessoSigiloRlsEnvelopeService.class);
        when(service.materialize(77L, "GET")).thenReturn(new ProcessoSigiloRlsEnvelope(
                77L,
                "tb_processo",
                "SIGILO_N2",
                "SIGILO_N2",
                "UNID-9",
                "TJCE",
                "PROC_SIGILO|TJCE|UNID-9|SIGILO_N2",
                true,
                true,
                true,
                false,
                true,
                List.of("DADOS_PESSOAIS", "DADOS_JUDICIAIS"),
                List.of(),
                null,
                null
        ));
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        PjbProcessoSigiloRlsFilter filter = new PjbProcessoSigiloRlsFilter(service, context);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/unificado/77/sigilo-inteligente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<PjbProcessoSigiloRlsContext.SessionSettings> capturedSettings = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedRequest.set(req);
            capturedSettings.set(context.current());
        };

        filter.doFilter(request, response, chain);

        HttpServletRequest effectiveRequest = (HttpServletRequest) capturedRequest.get();
        assertThat(effectiveRequest).isInstanceOf(PjbInstitutionalRoutingAugmentedRequest.class);
        assertThat(effectiveRequest.getHeader("X-PJB-Processo-Sigilo-Level")).isEqualTo("SIGILO_N2");
        assertThat(effectiveRequest.getHeader("X-PJB-Processo-Sigilo-Required-Clearance")).isEqualTo("SIGILO_N2");
        assertThat(effectiveRequest.getHeader("X-PJB-Processo-Sigilo-Rls-Scope")).isEqualTo("PROC_SIGILO|TJCE|UNID-9|SIGILO_N2");
        assertThat(effectiveRequest.getAttribute(PjbProcessoSigiloRlsFilter.ATTR_REQUIRED_UNIT_CODE)).isEqualTo("UNID-9");
        assertThat(response.getHeader("X-PJB-Processo-Sigilo-Classification")).isEqualTo("DADOS_PESSOAIS,DADOS_JUDICIAIS");
        assertThat(response.getHeader("X-PJB-Processo-Sigilo-Requires-Step-Up")).isEqualTo("true");
        assertThat(capturedSettings.get()).isNotNull();
        assertThat(capturedSettings.get().sigiloClearance()).isEqualTo("SIGILO_N2");
        assertThat(context.current()).isNull();
    }

    @Test
    void deveIgnorarRotasNaoRelacionadasAoFluxoSigiloso() throws Exception {
        ProcessoSigiloRlsEnvelopeService service = mock(ProcessoSigiloRlsEnvelopeService.class);
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        PjbProcessoSigiloRlsFilter filter = new PjbProcessoSigiloRlsFilter(service, context);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/consultas-publicas/search");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<PjbProcessoSigiloRlsContext.SessionSettings> capturedSettings = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedRequest.set(req);
            capturedSettings.set(context.current());
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedRequest.get()).isSameAs(request);
        assertThat(response.getHeaderNames()).isEmpty();
        assertThat(context.current()).isNull();
    }
}
