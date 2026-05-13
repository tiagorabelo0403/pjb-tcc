package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceCatalogProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRemoteProbeResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRuntimeSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRuntimeStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceRemoteProbeClient;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOfficialSourceConnectorProbeApplicationService {

    private final InstitutionalOfficialSourceCatalogService catalogService;
    private final InstitutionalOfficialSourceConnectorRegistry connectorRegistry;
    private final InstitutionalOfficialSourceConnectorProperties properties;
    private final InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository;
    private final InstitutionalOfficialSourceRemoteProbeClient remoteProbeClient;
    private final InstitutionalOfficialSourceConnectorCatalogApplicationService connectorCatalogApplicationService;
    private final Clock clock;

    public InstitutionalOfficialSourceConnectorProbeApplicationService(InstitutionalOfficialSourceCatalogService catalogService,
                                                                      InstitutionalOfficialSourceConnectorRegistry connectorRegistry,
                                                                      InstitutionalOfficialSourceConnectorProperties properties,
                                                                      InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository,
                                                                      InstitutionalOfficialSourceRemoteProbeClient remoteProbeClient,
                                                                      InstitutionalOfficialSourceConnectorCatalogApplicationService connectorCatalogApplicationService) {
        this(catalogService, connectorRegistry, properties, runtimeStateRepository, remoteProbeClient, connectorCatalogApplicationService, Clock.systemUTC());
    }

    InstitutionalOfficialSourceConnectorProbeApplicationService(InstitutionalOfficialSourceCatalogService catalogService,
                                                                InstitutionalOfficialSourceConnectorRegistry connectorRegistry,
                                                                InstitutionalOfficialSourceConnectorProperties properties,
                                                                InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository,
                                                                InstitutionalOfficialSourceRemoteProbeClient remoteProbeClient,
                                                                InstitutionalOfficialSourceConnectorCatalogApplicationService connectorCatalogApplicationService,
                                                                Clock clock) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.properties = Objects.requireNonNull(properties);
        this.runtimeStateRepository = Objects.requireNonNull(runtimeStateRepository);
        this.remoteProbeClient = Objects.requireNonNull(remoteProbeClient);
        this.connectorCatalogApplicationService = Objects.requireNonNull(connectorCatalogApplicationService);
        this.clock = Objects.requireNonNull(clock);
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse sondarTodos() {
        for (InstitutionalOfficialSourceCatalogProfile profile : catalogService.list()) {
            sondar(profile.sourceCode());
        }
        return connectorCatalogApplicationService.listar();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorResponse sondar(String sourceCode) {
        InstitutionalOfficialSourceCatalogProfile profile = catalogService.profileFor(sourceCode);
        InstitutionalOfficialSourceConnectorProfile current = connectorRegistry.describe(profile.sourceCode());
        Instant now = Instant.now(clock);
        Instant nextCheckAt = now.plus(Duration.ofMinutes(Math.max(5, properties.getProbeTtlMinutes())));
        runtimeStateRepository.save(buildSnapshot(profile, current, now, nextCheckAt));
        return connectorCatalogApplicationService.descrever(profile.sourceCode());
    }

    public int sondarRecorrencia(int batchSize) {
        int processed = 0;
        for (InstitutionalOfficialSourceCatalogProfile profile : catalogService.list()) {
            if (processed >= batchSize) {
                break;
            }
            InstitutionalOfficialSourceConnectorProfile current = connectorRegistry.describe(profile.sourceCode());
            if (current.enabled() && "PRONTO_PARA_VERIFICACAO_REMOTA".equals(current.connectorStatus())) {
                sondar(profile.sourceCode());
                processed++;
            }
        }
        return processed;
    }

    private InstitutionalOfficialSourceConnectorRuntimeSnapshot buildSnapshot(InstitutionalOfficialSourceCatalogProfile profile,
                                                                              InstitutionalOfficialSourceConnectorProfile current,
                                                                              Instant now,
                                                                              Instant nextCheckAt) {
        LinkedHashSet<String> signals = new LinkedHashSet<>(safe(current.signals()));
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(safe(current.fundamentos()));
        String status = current.connectorStatus();
        boolean liveVerificationSupported = current.liveVerificationSupported();
        URI target = targetUri(profile.sourceCode());
        if ("PRONTO_PARA_VERIFICACAO_REMOTA".equals(current.connectorStatus()) && target != null) {
            InstitutionalOfficialSourceConnectorRemoteProbeResult probe = remoteProbeClient.probe(
                    profile.sourceCode(),
                    target,
                    Duration.ofMillis(Math.max(1000, properties.getProbeTimeoutMillis()))
            );
            signals.addAll(safe(probe.signals()));
            blockers.addAll(safe(probe.blockers()));
            fundamentos.addAll(safe(probe.fundamentos()));
            if (probe.reachable() && probe.blockers().isEmpty()) {
                status = "VERIFICADO_REMOTO_OK";
                liveVerificationSupported = true;
            } else {
                status = probe.httpStatus() >= 500 || probe.httpStatus() <= 0 ? "VERIFICACAO_REMOTA_INDISPONIVEL" : "VERIFICADO_REMOTO_RESTRITO";
                liveVerificationSupported = false;
            }
            fundamentos.add("remote_probe_status=" + status);
            fundamentos.add("remote_probe_checked_at=" + now);
        } else if ("DRY_RUN_PREPARADO".equals(current.connectorStatus())) {
            status = "DRY_RUN_CONFIRMADO";
            fundamentos.add("remote_probe_status=DRY_RUN_CONFIRMADO");
            blockers.add("connector_still_in_dry_run");
            liveVerificationSupported = false;
        } else {
            fundamentos.add("remote_probe_status=" + status);
        }
        return new InstitutionalOfficialSourceConnectorRuntimeSnapshot(
                profile.sourceCode(),
                status,
                liveVerificationSupported,
                now,
                nextCheckAt,
                List.copyOf(sanitize(signals)),
                List.copyOf(sanitize(blockers)),
                List.copyOf(sanitize(fundamentos))
        );
    }

    private URI targetUri(String sourceCode) {
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = properties.getSources().get(normalize(sourceCode));
        if (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            return URI.create(config.getBaseUrl().trim());
        }
        InstitutionalOfficialSourceCatalogProfile profile = catalogService.profileFor(sourceCode);
        if (profile.officialReferenceUrl() != null && !profile.officialReferenceUrl().isBlank()) {
            return URI.create(profile.officialReferenceUrl().trim());
        }
        return null;
    }

    private static List<String> safe(List<String> source) {
        return source == null ? List.of() : source;
    }

    private static List<String> sanitize(LinkedHashSet<String> values) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return out;
    }

    private static String normalize(String sourceCode) {
        return sourceCode == null ? "" : sourceCode.trim().toUpperCase(Locale.ROOT);
    }
}
