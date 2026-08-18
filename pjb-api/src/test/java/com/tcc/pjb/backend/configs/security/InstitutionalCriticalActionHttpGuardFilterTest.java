package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalDocumentSecurityGate;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InstitutionalCriticalActionHttpGuardFilterTest {

    @Test
    void doFilterMustBindGateHeadersForCriticalAction() throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        when(gateService.enforce(eq("UNI-1"), eq("CAIXA-1"), eq(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO), eq("MP_MANIFESTACAO"), eq(true)))
                .thenReturn(new InstitutionalDocumentSecurityGate("MP_MANIFESTACAO", "aff-1", "nom-1", "UNI-1", "CAIXA-1", true, true, true, true, true, false, false, List.of(), List.of(), Instant.now()));
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/mp/manifestacao/99");
        request.addHeader("X-PJB-Institutional-Unit-Code", "UNI-1");
        request.addHeader("X-PJB-Institutional-Box-Code", "CAIXA-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation")).isEqualTo("MP_MANIFESTACAO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed")).isEqualTo("true");
    }

    @Test
    void doFilterMustReturnForbiddenWhenGateBlocksCriticalAction() throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        when(gateService.enforce(any(), any(), eq(InstitutionalSensitiveAct.REDISTRIBUICAO_SENSIVEL), eq("DISTRIBUICAO_REDISCRITICA"), eq(true)))
                .thenThrow(new RegraNegocioException("bloqueado"));
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/distribuicao/processual/processos/77/redistribuicao");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("INSTITUTIONAL_DOCUMENT_GATE_BLOCKED", "DISTRIBUICAO_REDISCRITICA");
    }

    @Test
    void mustProtectOfficialJusticeOfficeIssuance() throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        when(gateService.enforce(eq("UNI-77"), eq("CAIXA-OF"), eq(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO), eq("OFICIAL_OFICIO"), eq(true)))
                .thenReturn(new InstitutionalDocumentSecurityGate("OFICIAL_OFICIO", "aff-77", "nom-77", "UNI-77", "CAIXA-OF", true, true, true, true, true, false, false, List.of(), List.of(), Instant.now()));
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/oficial-justica/processos/44/oficios");
        request.addHeader("X-PJB-Institutional-Unit-Code", "UNI-77");
        request.addHeader("X-PJB-Institutional-Box-Code", "CAIXA-OF");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation")).isEqualTo("OFICIAL_OFICIO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed")).isEqualTo("true");
    }
    @Test
    void mustProtectUnifiedRecursalSurface() throws Exception {
        InstitutionalDocumentSecurityGateApplicationService gateService = mock(InstitutionalDocumentSecurityGateApplicationService.class);
        when(gateService.enforce(any(), any(), eq(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO), eq("RECURSAL_UNIFICADO"), eq(true)))
                .thenReturn(new InstitutionalDocumentSecurityGate("RECURSAL_UNIFICADO", "aff-r", "nom-r", null, null, true, true, true, true, true, false, false, List.of(), List.of(), Instant.now()));
        InstitutionalCriticalActionHttpGuardFilter filter = filterWith(gateService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/recursal/processos/55/recurso");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation")).isEqualTo("RECURSAL_UNIFICADO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed")).isEqualTo("true");
    }

    @SuppressWarnings("unchecked")
    private InstitutionalCriticalActionHttpGuardFilter filterWith(InstitutionalDocumentSecurityGateApplicationService gateService) {
        ObjectProvider<InstitutionalDocumentSecurityGateApplicationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(gateService);
        return new InstitutionalCriticalActionHttpGuardFilter(provider);
    }

}
