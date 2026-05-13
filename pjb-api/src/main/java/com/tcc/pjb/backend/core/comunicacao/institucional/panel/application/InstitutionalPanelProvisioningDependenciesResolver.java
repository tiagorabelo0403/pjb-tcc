package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class InstitutionalPanelProvisioningDependenciesResolver {

    private final InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService;
    private final InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService;
    private final InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService;
    private final InstitutionalHearingSchedulingGovernanceApplicationService hearingSchedulingGovernanceApplicationService;
    private final InstitutionalOperationalDeskGovernanceApplicationService operationalDeskGovernanceApplicationService;
    private final InstitutionalPanelProvisioningSupport support;

    InstitutionalPanelProvisioningDependenciesResolver(InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService,
                                                       InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
                                                       InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService,
                                                       InstitutionalHearingSchedulingGovernanceApplicationService hearingSchedulingGovernanceApplicationService,
                                                       InstitutionalOperationalDeskGovernanceApplicationService operationalDeskGovernanceApplicationService,
                                                       InstitutionalPanelProvisioningSupport support) {
        this.accessProfileCatalogApplicationService = Objects.requireNonNull(accessProfileCatalogApplicationService);
        this.panelBlueprintApplicationService = Objects.requireNonNull(panelBlueprintApplicationService);
        this.processWorkspaceApplicationService = Objects.requireNonNull(processWorkspaceApplicationService);
        this.hearingSchedulingGovernanceApplicationService = Objects.requireNonNull(hearingSchedulingGovernanceApplicationService);
        this.operationalDeskGovernanceApplicationService = Objects.requireNonNull(operationalDeskGovernanceApplicationService);
        this.support = Objects.requireNonNull(support);
    }

    InstitutionalPanelProvisioningContext resolve(InstitutionalOperationalProfileProjection profile) {
        InstitutionalAccessProfileCatalogEntry catalogEntry = resolveCatalogEntry(profile);
        InstitutionalProcessWorkspace workspace = resolveWorkspace(catalogEntry);
        InstitutionalHearingSchedulingGovernance hearingGovernance = hearingSchedulingGovernanceApplicationService.avaliar(profile, catalogEntry, workspace);
        InstitutionalOperationalDeskGovernance deskGovernance = operationalDeskGovernanceApplicationService.avaliar(profile, catalogEntry, workspace);
        String scope = support.firstNonBlank(profile.organizationScope(), catalogEntry == null ? null : support.inferScopeFromProfileCode(catalogEntry.codigo()));
        List<InstitutionalPanelBlueprintSpec> blueprints = panelBlueprintApplicationService.listar(scope, profile.panelCode());
        if (blueprints.isEmpty()) {
            blueprints = panelBlueprintApplicationService.listar(null, profile.panelCode());
        }
        return new InstitutionalPanelProvisioningContext(catalogEntry, workspace, hearingGovernance, deskGovernance, scope, blueprints);
    }

    private InstitutionalAccessProfileCatalogEntry resolveCatalogEntry(InstitutionalOperationalProfileProjection profile) {
        return accessProfileCatalogApplicationService.listarPerfis().stream()
                .map(entry -> new ScoredEntry(entry, support.score(entry, profile)))
                .filter(item -> item.score() > 0)
                .max(Comparator.comparingInt(ScoredEntry::score))
                .map(ScoredEntry::entry)
                .orElse(null);
    }

    private InstitutionalProcessWorkspace resolveWorkspace(InstitutionalAccessProfileCatalogEntry entry) {
        if (entry == null) {
            return null;
        }
        try {
            return processWorkspaceApplicationService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private record ScoredEntry(InstitutionalAccessProfileCatalogEntry entry, int score) {
    }
}
