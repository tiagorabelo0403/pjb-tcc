package com.tcc.pjb.backend.service.security.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalRequestContextKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrIdentityAssuranceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContextService;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundle;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PjbAuthenticatedSessionFacadeServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void mustFuseAuthenticationGovBrAndInstitutionalProfileSignalsIntoSingleSnapshot() {
        GovBrIdentityAssuranceApplicationService govBrService = mock(GovBrIdentityAssuranceApplicationService.class);
        InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
        NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService = mock(NationalCommunicationInstitutionalStateBundleFacadeService.class);
        PjbAuthenticatedSessionFacadeService service = new PjbAuthenticatedSessionFacadeService(
                new CurrentAuthenticationContextService(),
                govBrService,
                entryContextApplicationService,
                stateBundleFacadeService);

        when(govBrService.atual()).thenReturn(new GovBrIdentityAssuranceAggregate(
                true,
                9L,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                "SUBSTANCIAL",
                List.of(),
                List.of(),
                List.of("TOKEN_OK"),
                Instant.now()));

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "9",
                        "uid", "9",
                        "cpf", "12345678900",
                        "email", "usuario@mpce.mp.br",
                        "acr", "govbr_prata_loa2",
                        "amr", List.of("pwd", "mfa")));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBRO_MP"), new SimpleGrantedAuthority("ROLE_PJB_INSTITUCIONAL"))));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_PANEL_CODE, "PAINEL_UNIDADE");
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_LANDING_PATH, InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"));
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_PROFILE_STATE, "ATIVO_NO_PJB");
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_TARGET_ENVIRONMENT, "PAINEL_INSTITUCIONAL");
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_READ_REPLICA, "read-ce");
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_READY_FOR_PANEL, true);
        request.setAttribute(InstitutionalRequestContextKeys.ATTR_NOMINATION_ID, "NOM-9");
        request.setAttribute("PJB_DEVICE_ID", 77L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        InstitutionalEntrySummary summary = new InstitutionalEntrySummary(
                9L,
                "Usuário MP",
                TipoUsuario.PROMOTOR,
                null,
                true,
                true,
                List.of(),
                null,
                Instant.now());
        InstitutionalOperationalProfileProjection profile = new InstitutionalOperationalProfileProjection(
                "AFF-1|NOM-9",
                "ATIVO_NO_PJB",
                true,
                "AFF-1",
                "NOM-9",
                9L,
                "Usuário MP",
                "PROMOTOR",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO",
                "MPCE",
                "Promotoria de Morada Nova",
                "UNID-1",
                "1a Promotoria",
                "CX-1",
                "ATUACAO_MP",
                "MEMBRO_TITULAR",
                "PROMOTOR_TITULAR",
                "ATUACAO_MP",
                "PAINEL_UNIDADE",
                InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"),
                "amber",
                "ATUACAO_MP",
                "NIVEL_2_MFA_FORTE",
                true,
                true,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNID-1",
                "1a Promotoria",
                "Morada Nova",
                "CE|MPCE|UNID-1|CX-1|B2",
                "CE|MPCE|UNID-1|CX-1",
                "read-ce",
                List.of("ATUAR", "ASSINAR"),
                List.of("PJB"),
                List.of("PJB"),
                List.of(),
                List.of(),
                List.of("perfil_operacional_materializado"),
                Instant.now());
        InstitutionalEntryActivationDecision decision = new InstitutionalEntryActivationDecision(
                9L,
                "Usuário MP",
                "AFF-1",
                "NOM-9",
                "AFF-1|NOM-9",
                "ATIVO_NO_PJB",
                "PAINEL_INSTITUCIONAL",
                "INSTITUCIONAL_AFILIADO",
                "CTX-1",
                "PAINEL_UNIDADE",
                InstitutionalApiRoutes.painelExecutivoComUnidade("UNID-1"),
                "ATUACAO_MP",
                "UNID-1",
                "CX-1",
                "CE|MPCE|UNID-1|CX-1|B2",
                "read-ce",
                "BAIXO",
                12,
                "SUBSTANCIAL",
                "ASSINAR_MANIFESTACAO",
                null,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of("ATIVACAO_DIRETA_DO_PAINEL_INSTITUCIONAL_LIBERADA"),
                List.of("fundamento"),
                Instant.now());
        NationalCommunicationInstitutionalStateBundle stateBundle = new NationalCommunicationInstitutionalStateBundle(
                null,
                null,
                profile,
                new InstitutionalEntryActivationBundle(profile, decision));

        PjbAuthenticatedSessionResponse result = service.atual(summary, stateBundle);

        assertThat(result.authenticated()).isTrue();
        assertThat(result.jwtBacked()).isTrue();
        assertThat(result.mfaAtivo()).isTrue();
        assertThat(result.authenticationMethod()).isEqualTo("JWT_MFA");
        assertThat(result.activeDeviceId()).isEqualTo(77L);
        assertThat(result.govBrNivelGarantia()).isEqualTo("SUBSTANCIAL");
        assertThat(result.affiliationId()).isEqualTo("AFF-1");
        assertThat(result.nominationId()).isEqualTo("NOM-9");
        assertThat(result.panelCode()).isEqualTo("PAINEL_UNIDADE");
        assertThat(result.targetEnvironment()).isEqualTo("PAINEL_INSTITUCIONAL");
        assertThat(result.readReplicaCode()).isEqualTo("read-ce");
        assertThat(result.institutionalProfileVisible()).isTrue();
        assertThat(result.readyForInstitutionalPanel()).isTrue();
        assertThat(result.panelProvisioningComplete()).isTrue();
        assertThat(result.sharedExperienceReady()).isTrue();
        assertThat(result.activateInstitutionalContext()).isTrue();
        assertThat(result.evidencias()).contains("JWT_AUTENTICADO", "CONTA_GOVBR_VINCULADA", "PAINEL_CODE=PAINEL_UNIDADE");
    }
}
