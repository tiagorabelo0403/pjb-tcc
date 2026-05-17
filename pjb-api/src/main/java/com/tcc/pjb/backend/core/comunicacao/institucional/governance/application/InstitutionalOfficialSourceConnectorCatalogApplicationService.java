package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceCatalogProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class InstitutionalOfficialSourceConnectorCatalogApplicationService {

    private final InstitutionalOfficialSourceCatalogService catalogService;
    private final InstitutionalOfficialSourceConnectorRegistry connectorRegistry;
    private final Clock clock;

    @Inject
    public InstitutionalOfficialSourceConnectorCatalogApplicationService(InstitutionalOfficialSourceCatalogService catalogService,
                                                                        InstitutionalOfficialSourceConnectorRegistry connectorRegistry) {
        this(catalogService, connectorRegistry, Clock.systemUTC());
    }

    InstitutionalOfficialSourceConnectorCatalogApplicationService(InstitutionalOfficialSourceCatalogService catalogService,
                                                                  InstitutionalOfficialSourceConnectorRegistry connectorRegistry,
                                                                  Clock clock) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse listar() {
        List<NationalCommunicationInstitutionalOfficialSourceConnectorResponse> items = catalogService.list().stream()
                .map(this::toResponse)
                .toList();
        return new NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse(Instant.now(clock), items);
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorResponse descrever(String sourceCode) {
        return toResponse(catalogService.profileFor(sourceCode));
    }

    private NationalCommunicationInstitutionalOfficialSourceConnectorResponse toResponse(InstitutionalOfficialSourceCatalogProfile profile) {
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe(profile.sourceCode());
        return new NationalCommunicationInstitutionalOfficialSourceConnectorResponse(
                profile.sourceCode(),
                profile.sourceLabel(),
                profile.authority(),
                profile.authorityScope(),
                profile.accessMode(),
                profile.refreshMode(),
                profile.directGovernmentSource(),
                profile.autoRefreshSupported(),
                profile.baseConfidence(),
                profile.officialReferenceUrl(),
                connector.connectorStatus(),
                connector.enabled(),
                connector.liveVerificationSupported(),
                connector.referenceUrl(),
                connector.checkedAt(),
                connector.nextCheckAt(),
                connector.signals(),
                connector.blockers(),
                connector.fundamentos()
        );
    }
}
