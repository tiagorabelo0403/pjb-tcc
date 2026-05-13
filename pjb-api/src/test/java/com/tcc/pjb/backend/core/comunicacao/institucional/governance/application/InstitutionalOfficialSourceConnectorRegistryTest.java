package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRuntimeStateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InstitutionalOfficialSourceConnectorRegistryTest {

    @Test
    void shouldMarkRemoteConnectorAsReadyWhenBaseUrlExistsAndDryRunIsOff() {
        InstitutionalOfficialSourceCatalogService catalogService = new InstitutionalOfficialSourceCatalogService();
        InstitutionalOfficialSourceConnectorProperties properties = new InstitutionalOfficialSourceConnectorProperties();
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        config.setBaseUrl("https://cnj.example.internal");
        config.setDryRun(false);
        properties.getSources().put("CNJ_DATAJUD", config);
        InstitutionalOfficialSourceConnectorRegistry registry = new InstitutionalOfficialSourceConnectorRegistry(
                catalogService,
                properties,
                new InstitutionalOfficialSourceConnectorRuntimeStateRepository(),
                Clock.fixed(Instant.parse("2026-04-05T18:00:00Z"), ZoneOffset.UTC));

        var profile = registry.describe("CNJ_DATAJUD");

        assertEquals("PRONTO_PARA_VERIFICACAO_REMOTA", profile.connectorStatus());
        assertTrue(profile.liveVerificationSupported());
        assertTrue(profile.blockers().isEmpty());
    }

    @Test
    void shouldForceHumanHomologationForManualSources() {
        InstitutionalOfficialSourceCatalogService catalogService = new InstitutionalOfficialSourceCatalogService();
        InstitutionalOfficialSourceConnectorRegistry registry = new InstitutionalOfficialSourceConnectorRegistry(
                catalogService,
                new InstitutionalOfficialSourceConnectorProperties(),
                new InstitutionalOfficialSourceConnectorRuntimeStateRepository(),
                Clock.fixed(Instant.parse("2026-04-05T18:00:00Z"), ZoneOffset.UTC));

        var profile = registry.describe("ATO_PUBLICADO");

        assertEquals("HOMOLOGACAO_HUMANA_OBRIGATORIA", profile.connectorStatus());
        assertFalse(profile.liveVerificationSupported());
    }
}
