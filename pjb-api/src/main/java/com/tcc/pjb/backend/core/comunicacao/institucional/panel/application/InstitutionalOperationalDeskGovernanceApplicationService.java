package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOperationalDeskGovernanceApplicationService {

    private final InstitutionalOperationalDeskSnapshotResolver snapshotResolver;
    private final InstitutionalOperationalDeskGovernanceAssembler governanceAssembler;

    public InstitutionalOperationalDeskGovernanceApplicationService() {
        this(new InstitutionalOperationalDeskSupport());
    }

    InstitutionalOperationalDeskGovernanceApplicationService(InstitutionalOperationalDeskSupport support) {
        this(new InstitutionalOperationalDeskSnapshotResolver(support), new InstitutionalOperationalDeskGovernanceAssembler(support));
    }

    InstitutionalOperationalDeskGovernanceApplicationService(InstitutionalOperationalDeskSnapshotResolver snapshotResolver,
                                                             InstitutionalOperationalDeskGovernanceAssembler governanceAssembler) {
        this.snapshotResolver = Objects.requireNonNull(snapshotResolver);
        this.governanceAssembler = Objects.requireNonNull(governanceAssembler);
    }

    public InstitutionalOperationalDeskGovernance avaliar(InstitutionalOperationalProfileProjection profile,
                                                          InstitutionalAccessProfileCatalogEntry catalogEntry,
                                                          InstitutionalProcessWorkspace workspace) {
        if (profile == null) {
            return governanceAssembler.missingProfile();
        }
        InstitutionalOperationalDeskSnapshot snapshot = snapshotResolver.resolve(profile, catalogEntry, workspace);
        return governanceAssembler.assemble(snapshot, workspace);
    }
}
