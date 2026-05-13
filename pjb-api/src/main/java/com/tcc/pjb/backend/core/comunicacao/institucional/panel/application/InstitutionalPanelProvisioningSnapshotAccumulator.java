package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.List;

final class InstitutionalPanelProvisioningSnapshotAccumulator {

    private final InstitutionalPanelProvisioningSupport support;

    InstitutionalPanelProvisioningSnapshotAccumulator(InstitutionalPanelProvisioningSupport support) {
        this.support = support;
    }

    InstitutionalPanelProvisioningSnapshot accumulate(List<InstitutionalPanelBlueprintSpec> blueprints,
                                                      InstitutionalProcessWorkspace workspace) {
        LinkedHashSet<String> primarySections = new LinkedHashSet<>();
        LinkedHashSet<String> quickActions = new LinkedHashSet<>();
        LinkedHashSet<String> securityGuards = new LinkedHashSet<>();
        LinkedHashSet<String> visibilityRules = new LinkedHashSet<>();
        LinkedHashSet<String> tabs = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String initialRoute = null;
        for (InstitutionalPanelBlueprintSpec blueprint : blueprints) {
            if (support.isBlank(initialRoute) && !support.isBlank(blueprint.rotaInicial())) {
                initialRoute = blueprint.rotaInicial();
            }
            primarySections.addAll(blueprint.secoesPrimarias());
            quickActions.addAll(blueprint.acoesRapidas());
            securityGuards.addAll(blueprint.guardasSeguranca());
            visibilityRules.addAll(blueprint.regrasVisibilidade());
            fundamentos.addAll(blueprint.fundamentos());
        }
        if (workspace != null) {
            tabs.addAll(workspace.tabs());
            fundamentos.addAll(workspace.fundamentos());
        }
        return new InstitutionalPanelProvisioningSnapshot(
                initialRoute,
                primarySections,
                quickActions,
                securityGuards,
                visibilityRules,
                tabs,
                fundamentos
        );
    }
}
