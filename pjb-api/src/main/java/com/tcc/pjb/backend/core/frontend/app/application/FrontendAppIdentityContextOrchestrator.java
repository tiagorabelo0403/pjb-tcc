package com.tcc.pjb.backend.core.frontend.app.application;

import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.service.profile.surface.PerfilCapabilitySurfaceFacadeService;
import com.tcc.pjb.backend.service.security.surface.SecurityContextSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: agrupa os 4 servicos que respondem
 * pela identidade e contexto de segurança do usuário — capacidade de perfil, contexto
 * institucional, extração e política de asseguramento Gov.br.
 */
@Service
public class FrontendAppIdentityContextOrchestrator {

    private final PerfilCapabilitySurfaceFacadeService capabilitySurfaceFacadeService;
    private final SecurityContextSurfaceFacadeService securityContextSurfaceFacadeService;
    private final GovBrAssuranceExtractor govBrAssuranceExtractor;
    private final GovBrAssurancePolicy govBrAssurancePolicy;

    public FrontendAppIdentityContextOrchestrator(PerfilCapabilitySurfaceFacadeService capabilitySurfaceFacadeService,
                                                   SecurityContextSurfaceFacadeService securityContextSurfaceFacadeService,
                                                   GovBrAssuranceExtractor govBrAssuranceExtractor,
                                                   GovBrAssurancePolicy govBrAssurancePolicy) {
        this.capabilitySurfaceFacadeService = Objects.requireNonNull(capabilitySurfaceFacadeService);
        this.securityContextSurfaceFacadeService = Objects.requireNonNull(securityContextSurfaceFacadeService);
        this.govBrAssuranceExtractor = Objects.requireNonNull(govBrAssuranceExtractor);
        this.govBrAssurancePolicy = Objects.requireNonNull(govBrAssurancePolicy);
    }

    public String resolveAssurance(Authentication authentication) {
        return govBrAssuranceExtractor.extract(authentication);
    }

    public boolean stepUpRequired(String assurance) {
        return govBrAssurancePolicy.exigeStepUp(assurance, true);
    }

    public CapabilityExtensionResponse loadCapabilities() {
        return capabilitySurfaceFacadeService.capacidades(null);
    }

    public SecurityContextResponse loadSecurityContext(HttpServletRequest request) {
        return securityContextSurfaceFacadeService.context(request);
    }
}
