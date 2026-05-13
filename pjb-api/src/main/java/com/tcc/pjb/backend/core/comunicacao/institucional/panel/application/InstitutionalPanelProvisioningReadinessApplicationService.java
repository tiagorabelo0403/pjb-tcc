package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import java.time.InstantSource;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPanelProvisioningReadinessApplicationService {

    private static final String CALENDAR_SURFACE_ROUTE = "/api/v1/calendar/events";
    private static final String HEARING_SURFACE_ROUTE = "/api/v1/institutional/hearings";
    private static final String READING_SURFACE_ROUTE = "/api/v1/processual/reading-mode";
    private static final String TRIAGE_SURFACE_ROUTE = "/api/v1/institutional/triage";
    private static final String CALCULATOR_SURFACE_ROUTE = "/api/v1/processual/calculos/workspace";

    private final InstantSource instantSource;
    private final InstitutionalPanelProvisioningSupport support;
    private final InstitutionalPanelProvisioningDependenciesResolver dependenciesResolver;
    private final InstitutionalPanelProvisioningSnapshotAccumulator snapshotAccumulator;
    private final InstitutionalPanelProvisioningOutcomeFactory outcomeFactory;

    public InstitutionalPanelProvisioningReadinessApplicationService(InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService,
                                                                     InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
                                                                     InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService,
                                                                     InstitutionalHearingSchedulingGovernanceApplicationService hearingSchedulingGovernanceApplicationService,
                                                                     InstitutionalOperationalDeskGovernanceApplicationService operationalDeskGovernanceApplicationService) {
        this(
                accessProfileCatalogApplicationService,
                panelBlueprintApplicationService,
                processWorkspaceApplicationService,
                hearingSchedulingGovernanceApplicationService,
                operationalDeskGovernanceApplicationService,
                InstantSource.system()
        );
    }

    InstitutionalPanelProvisioningReadinessApplicationService(InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService,
                                                              InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
                                                              InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService,
                                                              InstitutionalHearingSchedulingGovernanceApplicationService hearingSchedulingGovernanceApplicationService,
                                                              InstitutionalOperationalDeskGovernanceApplicationService operationalDeskGovernanceApplicationService,
                                                              InstantSource instantSource) {
        this.instantSource = Objects.requireNonNull(instantSource);
        this.support = new InstitutionalPanelProvisioningSupport();
        this.dependenciesResolver = new InstitutionalPanelProvisioningDependenciesResolver(
                accessProfileCatalogApplicationService,
                panelBlueprintApplicationService,
                processWorkspaceApplicationService,
                hearingSchedulingGovernanceApplicationService,
                operationalDeskGovernanceApplicationService,
                support
        );
        this.snapshotAccumulator = new InstitutionalPanelProvisioningSnapshotAccumulator(support);
        this.outcomeFactory = new InstitutionalPanelProvisioningOutcomeFactory(support);
    }

    public InstitutionalPanelProvisioningReadiness avaliar(InstitutionalOperationalProfileProjection profile) {
        if (profile == null || support.isBlank(profile.panelCode())) {
            return null;
        }
        InstitutionalPanelProvisioningContext context = dependenciesResolver.resolve(profile);
        InstitutionalPanelProvisioningSnapshot snapshot = snapshotAccumulator.accumulate(context.blueprints(), context.workspace());
        return outcomeFactory.create(profile, context, snapshot, instantSource.instant());
    }
}
