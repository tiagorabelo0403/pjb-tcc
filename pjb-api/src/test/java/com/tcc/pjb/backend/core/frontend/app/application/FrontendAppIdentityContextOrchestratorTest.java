package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.service.profile.surface.PerfilCapabilitySurfaceFacadeService;
import com.tcc.pjb.backend.service.security.surface.SecurityContextSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

class FrontendAppIdentityContextOrchestratorTest {

    private final PerfilCapabilitySurfaceFacadeService capabilitySurface = mock(PerfilCapabilitySurfaceFacadeService.class);
    private final SecurityContextSurfaceFacadeService securityContextSurface = mock(SecurityContextSurfaceFacadeService.class);
    private final GovBrAssuranceExtractor assuranceExtractor = mock(GovBrAssuranceExtractor.class);
    private final GovBrAssurancePolicy assurancePolicy = mock(GovBrAssurancePolicy.class);
    private final FrontendAppIdentityContextOrchestrator orchestrator = new FrontendAppIdentityContextOrchestrator(
            capabilitySurface, securityContextSurface, assuranceExtractor, assurancePolicy);

    @Test
    void resolveAssuranceDelegaAoExtractor() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        when(assuranceExtractor.extract(auth)).thenReturn("ouro");
        assertThat(orchestrator.resolveAssurance(auth)).isEqualTo("ouro");
    }

    @Test
    void stepUpRequiredDelegaAPolicyComRequiresTrueFixo() {
        when(assurancePolicy.exigeStepUp("prata", true)).thenReturn(true);
        when(assurancePolicy.exigeStepUp("ouro", true)).thenReturn(false);
        assertThat(orchestrator.stepUpRequired("prata")).isTrue();
        assertThat(orchestrator.stepUpRequired("ouro")).isFalse();
    }

    @Test
    void loadCapabilitiesDelegaComNullFixo() {
        var expected = new CapabilityExtensionResponse("CIDADAO", List.of("A", "B"));
        when(capabilitySurface.capacidades(null)).thenReturn(expected);
        assertThat(orchestrator.loadCapabilities()).isSameAs(expected);
    }

    @Test
    void loadSecurityContextDelegaComRequest() {
        var request = new MockHttpServletRequest();
        var expected = mock(SecurityContextResponse.class);
        when(securityContextSurface.context(request)).thenReturn(expected);
        assertThat(orchestrator.loadSecurityContext(request)).isSameAs(expected);
    }
}
