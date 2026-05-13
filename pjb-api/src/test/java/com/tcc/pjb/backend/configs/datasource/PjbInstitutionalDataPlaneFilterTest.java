package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundle;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PjbInstitutionalDataPlaneFilterTest {

    @Test
    void mustInjectSyntheticRoutingHeadersAndResponseHintsForCanonicalInstitutionalRequests() throws Exception {
        NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService = mock(NationalCommunicationInstitutionalStateBundleFacadeService.class);
        InstitutionalAccessContextMaterializationApplicationService accessContextService = mock(InstitutionalAccessContextMaterializationApplicationService.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbInstitutionalDataPlaneFilter filter = new PjbInstitutionalDataPlaneFilter(stateBundleFacadeService, accessContextService, properties);
        InstitutionalHorizontalDataPlanePlan plan = new InstitutionalHorizontalDataPlanePlan(
                "AFF-1|NOM-9",
                "AFF-1",
                "NOM-9",
                "FORUM",
                "FORUM",
                "Morada Nova",
                "CE",
                "TJCE",
                "UNID-1",
                "1a Vara",
                "Morada Nova",
                "CX-1",
                "PAINEL_UNIDADE",
                InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"),
                true,
                false,
                true,
                "LOCAL",
                "CE|TJCE|UNID-1|CX-1|B7",
                "CE|TJCE|UNID-1|CX-1",
                "read-ce",
                7,
                64,
                "CE|TJCE|UNID-1|ARQUIVO",
                List.of("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA"),
                Map.of(
                        "X-PJB-UF", "CE",
                        "X-PJB-Tribunal", "TJCE",
                        "X-PJB-Orgao", "TJCE",
                        "X-PJB-Unidade", "UNID-1",
                        "X-PJB-Caixa", "CX-1",
                        "X-PJB-Read-Replica", "read-ce"
                ),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of("PJB"),
                List.of("DIRETOR_GERAL"),
                List.of("aprovacao_pendente=DIRETOR_GERAL"),
                List.of("fundamento"),
                Instant.now());
        InstitutionalOperationalProfileProjection profileProjection = new InstitutionalOperationalProfileProjection(
                "AFF-1|NOM-9",
                "AGUARDANDO_GOVERNANCA",
                true,
                "AFF-1",
                "NOM-9",
                9L,
                "Usuario",
                "SERVIDOR_FORUM",
                "FORUM",
                "FORUM",
                "TJCE",
                "Forum de Morada Nova",
                "UNID-1",
                "1a Vara",
                "CX-1",
                "SECRETARIA",
                "SECRETARIA_FORUM",
                "GESTOR_CAIXA",
                "SECRETARIA_FORUM",
                "PAINEL_UNIDADE",
                InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"),
                "amber",
                "SECRETARIA_FORUM",
                "NIVEL_2_MFA_FORTE",
                true,
                false,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNID-1",
                "1a Vara",
                "Morada Nova",
                "CE|TJCE|UNID-1|CX-1|B7",
                "CE|TJCE|UNID-1|CX-1",
                "read-ce",
                List.of("VISUALIZAR"),
                List.of("PJB"),
                List.of(),
                List.of("PJB"),
                List.of("aprovacao_pendente=PJB"),
                List.of("fundamento"),
                Instant.now());
        InstitutionalEntryActivationBundle activationBundle = new InstitutionalEntryActivationBundle(
                null,
                new InstitutionalEntryActivationDecision(
                        9L,
                        "Usuario",
                        "AFF-1",
                        "NOM-9",
                        "AFF-1|NOM-9",
                        "AGUARDANDO_GOVERNANCA",
                        "AGUARDANDO_APROVACAO_MANUAL",
                        "DIRETO_PESSOA",
                        "CTX-1",
                        "PAINEL_UNIDADE",
                        InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"),
                        "SECRETARIA_FORUM",
                        "UNID-1",
                        "CX-1",
                        "CE|TJCE|UNID-1|CX-1|B7",
                        "read-ce",
                        "MEDIO",
                        44,
                        "SUBSTANCIAL",
                        "ASSINAR_MANIFESTACAO",
                        "/api/v1/auth/govbr/stepup/start",
                        true,
                        true,
                        false,
                        true,
                        true,
                        false,
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        List.of("PENDENCIA_GOVERNANCA"),
                        List.of("PENDENCIA_GOVERNANCA"),
                        List.of("NOVO_DISPOSITIVO"),
                        List.of("PERFIL_OPERACIONAL_VISIVEL_NO_PJB"),
                        List.of("fundamento"),
                        Instant.now()));
        when(stateBundleFacadeService.carregar("AFF-1", "NOM-9")).thenReturn(new NationalCommunicationInstitutionalStateBundle(null, plan, profileProjection, activationBundle));
        when(accessContextService.materializar("AFF-1", "NOM-9")).thenReturn(new InstitutionalAccessContextSnapshot(
                "AFF-1|NOM-9",
                "AFF-1",
                "NOM-9",
                "PAINEL_UNIDADE",
                "SECRETARIA_FORUM",
                "UNID-1",
                "CX-1",
                "LOCAL",
                "CE|TJCE|UNID-1|CX-1|B7",
                "CE|TJCE|UNID-1|CX-1",
                "read-ce",
                "NIVEL_2_MFA_FORTE",
                true,
                true,
                false,
                true,
                true,
                false,
                "AFF-1|UNID-1|CX-1",
                List.of("UNID-1"),
                List.of("CX-1"),
                List.of("LANE-1"),
                List.of("DEL-1"),
                List.of("PJB"),
                Map.of("X-PJB-RLS-Scope", "AFF-1|UNID-1|CX-1"),
                List.of("ok"),
                List.of("fundamento"),
                Instant.now()));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", InstitutionalApiRoutes.planoDadosHorizontal("NOM-9"));
        request.setParameter("affiliationId", "AFF-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<ServletResponse> capturedResponse = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedRequest.set(req);
            capturedResponse.set(res);
        };

        filter.doFilter(request, response, chain);

        verify(stateBundleFacadeService).carregar("AFF-1", "NOM-9");
        assertThat(capturedRequest.get()).isInstanceOf(PjbInstitutionalRoutingAugmentedRequest.class);
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getHeader("X-PJB-UF")).isEqualTo("CE");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getHeader("X-PJB-Read-Replica")).isEqualTo("read-ce");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_PANEL_CODE)).isEqualTo("PAINEL_UNIDADE");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_DATA_PLANE_KEY)).isEqualTo("CE|TJCE|UNID-1|CX-1|B7");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_READY_FOR_PANEL)).isEqualTo(true);
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_PANEL_PROVISIONING_COMPLETE)).isEqualTo(true);
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_SHARED_EXPERIENCE_READY)).isEqualTo(true);
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_PROFILE_STATE)).isEqualTo("AGUARDANDO_GOVERNANCA");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_TARGET_ENVIRONMENT)).isEqualTo("AGUARDANDO_APROVACAO_MANUAL");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_RLS_SCOPE_KEY)).isEqualTo("AFF-1|UNID-1|CX-1");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_COVERAGE_MODE)).isEqualTo("LOCAL");
        assertThat(((jakarta.servlet.http.HttpServletRequest) capturedRequest.get()).getAttribute(PjbInstitutionalDataPlaneFilter.ATTR_ACCESS_REQUIRES_STEP_UP)).isEqualTo(true);
        assertThat(capturedResponse.get()).isSameAs(response);
        assertThat(response.getHeader("X-PJB-Institutional-Panel-Code")).isEqualTo("PAINEL_UNIDADE");
        assertThat(response.getHeader("X-PJB-Institutional-Landing-Path")).isEqualTo(InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"));
        assertThat(response.getHeader("X-PJB-Institutional-Data-Plane-Key")).isEqualTo("CE|TJCE|UNID-1|CX-1|B7");
        assertThat(response.getHeader("X-PJB-Institutional-Read-Replica")).isEqualTo("read-ce");
        assertThat(response.getHeader("X-PJB-Institutional-Ready-For-Panel")).isEqualTo("true");
        assertThat(response.getHeader("X-PJB-Institutional-Profile-State")).isEqualTo("AGUARDANDO_GOVERNANCA");
        assertThat(response.getHeader("X-PJB-Institutional-Target-Environment")).isEqualTo("AGUARDANDO_APROVACAO_MANUAL");
        assertThat(response.getHeader("X-PJB-Institutional-Rls-Scope")).isEqualTo("AFF-1|UNID-1|CX-1");
        assertThat(response.getHeader("X-PJB-Institutional-Coverage-Mode")).isEqualTo("LOCAL");
        assertThat(response.getHeader("X-PJB-Institutional-Requires-Step-Up")).isEqualTo("true");
    }

    @Test
    void mustIgnoreLegacyAndNonInstitutionalPaths() throws Exception {
        NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService = mock(NationalCommunicationInstitutionalStateBundleFacadeService.class);
        InstitutionalAccessContextMaterializationApplicationService accessContextService = mock(InstitutionalAccessContextMaterializationApplicationService.class);
        PjbInstitutionalDataPlaneFilter filter = new PjbInstitutionalDataPlaneFilter(stateBundleFacadeService, accessContextService, new PjbDataSourceRoutingProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/processual/comunicacoes/institucional/painel-executivo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedRequest.set(req);

        filter.doFilter(request, response, chain);

        assertThat(capturedRequest.get()).isSameAs(request);
    }


    @Test
    void mustResolveAffiliationIdFromCanonicalPathWithoutQueryParameter() throws Exception {
        NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService = mock(NationalCommunicationInstitutionalStateBundleFacadeService.class);
        InstitutionalAccessContextMaterializationApplicationService accessContextService = mock(InstitutionalAccessContextMaterializationApplicationService.class);
        PjbInstitutionalDataPlaneFilter filter = new PjbInstitutionalDataPlaneFilter(stateBundleFacadeService, accessContextService, new PjbDataSourceRoutingProperties());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", InstitutionalApiRoutes.homologarAfiliacao("AFF-77"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };

        filter.doFilter(request, response, chain);

        verify(stateBundleFacadeService).carregar("AFF-77", null);
    }
}
