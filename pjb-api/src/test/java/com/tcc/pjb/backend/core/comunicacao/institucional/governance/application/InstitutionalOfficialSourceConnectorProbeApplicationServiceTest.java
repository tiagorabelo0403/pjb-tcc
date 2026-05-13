package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRemoteProbeResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRuntimeStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceRemoteProbeClient;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalOfficialSourceConnectorProbeApplicationServiceTest {

    @Test
    void shouldUpgradeCatalogStatusToRemoteVerifiedAfterSuccessfulProbe() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-05T18:00:00Z"), ZoneOffset.UTC);
        InstitutionalOfficialSourceCatalogService catalogService = new InstitutionalOfficialSourceCatalogService();
        InstitutionalOfficialSourceConnectorProperties properties = new InstitutionalOfficialSourceConnectorProperties();
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        config.setBaseUrl("https://cnj.example.internal/status");
        config.setDryRun(false);
        properties.getSources().put("CNJ_DATAJUD", config);
        InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository = new InstitutionalOfficialSourceConnectorRuntimeStateRepository();
        InstitutionalOfficialSourceConnectorRegistry registry = new InstitutionalOfficialSourceConnectorRegistry(catalogService, properties, runtimeStateRepository, clock);
        InstitutionalOfficialSourceConnectorCatalogApplicationService catalogApplicationService = new InstitutionalOfficialSourceConnectorCatalogApplicationService(catalogService, registry, clock);
        InstitutionalOfficialSourceRemoteProbeClient remoteProbeClient = new InstitutionalOfficialSourceRemoteProbeClient() {
            @Override
            public InstitutionalOfficialSourceConnectorRemoteProbeResult probe(String sourceCode, URI target, java.time.Duration timeout) {
                return new InstitutionalOfficialSourceConnectorRemoteProbeResult(true, 200, 42, List.of("probe_ok"), List.of(), List.of("fundamento_probe_ok"));
            }
        };
        InstitutionalOfficialSourceConnectorProbeApplicationService service = new InstitutionalOfficialSourceConnectorProbeApplicationService(
                catalogService,
                registry,
                properties,
                runtimeStateRepository,
                remoteProbeClient,
                catalogApplicationService,
                clock
        );

        var response = service.sondar("CNJ_DATAJUD");

        assertEquals("VERIFICADO_REMOTO_OK", response.connectorStatus());
        assertTrue(response.connectorLiveVerificationSupported());
        assertTrue(response.connectorSignals().contains("probe_ok"));
    }

    @Test
    void shouldKeepDryRunRestrictedWhenConnectorIsNotLiveReady() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-05T18:00:00Z"), ZoneOffset.UTC);
        InstitutionalOfficialSourceCatalogService catalogService = new InstitutionalOfficialSourceCatalogService();
        InstitutionalOfficialSourceConnectorProperties properties = new InstitutionalOfficialSourceConnectorProperties();
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        config.setBaseUrl("https://receita.example.internal/status");
        config.setDryRun(true);
        properties.getSources().put("RECEITA_CNPJ", config);
        InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository = new InstitutionalOfficialSourceConnectorRuntimeStateRepository();
        InstitutionalOfficialSourceConnectorRegistry registry = new InstitutionalOfficialSourceConnectorRegistry(catalogService, properties, runtimeStateRepository, clock);
        InstitutionalOfficialSourceConnectorCatalogApplicationService catalogApplicationService = new InstitutionalOfficialSourceConnectorCatalogApplicationService(catalogService, registry, clock);
        InstitutionalOfficialSourceRemoteProbeClient remoteProbeClient = new InstitutionalOfficialSourceRemoteProbeClient() {
            @Override
            public InstitutionalOfficialSourceConnectorRemoteProbeResult probe(String sourceCode, URI target, java.time.Duration timeout) {
                throw new AssertionError("dry-run connector should not execute live probe");
            }
        };
        InstitutionalOfficialSourceConnectorProbeApplicationService service = new InstitutionalOfficialSourceConnectorProbeApplicationService(
                catalogService,
                registry,
                properties,
                runtimeStateRepository,
                remoteProbeClient,
                catalogApplicationService,
                clock
        );

        var response = service.sondar("RECEITA_CNPJ");

        assertEquals("DRY_RUN_CONFIRMADO", response.connectorStatus());
        assertFalse(response.connectorLiveVerificationSupported());
        assertTrue(response.connectorBlockers().contains("connector_still_in_dry_run"));
    }
}
