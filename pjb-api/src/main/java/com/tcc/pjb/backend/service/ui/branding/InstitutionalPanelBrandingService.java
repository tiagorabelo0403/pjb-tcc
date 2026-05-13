package com.tcc.pjb.backend.service.ui.branding;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPanelBrandingService {

    private final InstitutionalBrandingResolverService institutionalBrandingResolverService;
    private final InstitutionalBrandingPolicyService institutionalBrandingPolicyService;
    private final InstitutionalPanelVisualComposerService institutionalPanelVisualComposerService;

    public InstitutionalPanelBrandingService(InstitutionalBrandingResolverService institutionalBrandingResolverService,
                                             InstitutionalBrandingPolicyService institutionalBrandingPolicyService,
                                             InstitutionalPanelVisualComposerService institutionalPanelVisualComposerService) {
        this.institutionalBrandingResolverService = Objects.requireNonNull(institutionalBrandingResolverService, "institutionalBrandingResolverService");
        this.institutionalBrandingPolicyService = Objects.requireNonNull(institutionalBrandingPolicyService, "institutionalBrandingPolicyService");
        this.institutionalPanelVisualComposerService = Objects.requireNonNull(institutionalPanelVisualComposerService, "institutionalPanelVisualComposerService");
    }

    public Map<String, Object> resolve(String actorLane, String panelKind, TipoUsuario tipoUsuario) {
        Map<String, Object> institutionalBranding = institutionalBrandingResolverService.resolveProfile(
                new InstitutionalBrandingResolverService.ResolveRequest(actorLane, panelKind, tipoUsuario, Map.of())
        );
        Map<String, Object> panelVisualIdentity = institutionalPanelVisualComposerService.compose(
                new InstitutionalPanelVisualComposerService.ResolveRequest(actorLane, panelKind, institutionalBranding)
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("institutionalBranding", institutionalBranding);
        out.put("panelVisualIdentity", panelVisualIdentity);
        out.put("brandingGovernance", institutionalBrandingPolicyService.governanceSummary());
        return Collections.unmodifiableMap(out);
    }
}
