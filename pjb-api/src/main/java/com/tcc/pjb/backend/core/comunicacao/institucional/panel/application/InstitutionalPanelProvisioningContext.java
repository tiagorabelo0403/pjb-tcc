package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.List;

record InstitutionalPanelProvisioningContext(InstitutionalAccessProfileCatalogEntry catalogEntry,
                                             InstitutionalProcessWorkspace workspace,
                                             InstitutionalHearingSchedulingGovernance hearingGovernance,
                                             InstitutionalOperationalDeskGovernance deskGovernance,
                                             String scope,
                                             List<InstitutionalPanelBlueprintSpec> blueprints) {
}
