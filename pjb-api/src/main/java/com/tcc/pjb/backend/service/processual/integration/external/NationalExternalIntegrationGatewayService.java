package com.tcc.pjb.backend.service.processual.integration.external;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.processual.integration.external.ExternalIntegrationDiagnosticRequest;
import com.tcc.pjb.backend.model.dto.processual.integration.external.ExternalIntegrationDiagnosticResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class NationalExternalIntegrationGatewayService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final JudicialConnectorRegistry judicialConnectorRegistry;
    private final JudicialConnectorReadinessService readinessService;
    private final JudicialConnectorHomologationService homologationService;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public NationalExternalIntegrationGatewayService(ProcessoRepository processoRepository,
                                                     PjbAuthorizationService authorizationService,
                                                     TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                                     JudicialConnectorRegistry judicialConnectorRegistry,
                                                     JudicialConnectorReadinessService readinessService,
                                                     JudicialConnectorHomologationService homologationService,
                                                     JudicialConnectorOperationalProfileService operationalProfileService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.judicialConnectorRegistry = Objects.requireNonNull(judicialConnectorRegistry);
        this.readinessService = Objects.requireNonNull(readinessService);
        this.homologationService = Objects.requireNonNull(homologationService);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public ExternalIntegrationDiagnosticResponse diagnosticar(ExternalIntegrationDiagnosticRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        Map<String, Object> payload = buildPayload(processo);
        TribunalProtocolRoutingService.RoutingDecision routingDecision = tribunalProtocolRoutingService.resolve(
                payload,
                processo.getRito(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                isRecursal(processo)
        );
        if (routingDecision == null) {
            JudicialSystem fallbackSystem = parseSystem(processo.getConnectorSystem()).orElse(JudicialSystem.PJE);
            JudicialSubmissionCapability fallbackCapability = judicialConnectorRegistry.find(fallbackSystem)
                    .map(JudicialProcessConnector::capability)
                    .orElse(new JudicialSubmissionCapability(fallbackSystem, true, false, false, false, false, false, false, false, List.of(), List.of(), List.of(), null));
            routingDecision = new TribunalProtocolRoutingService.RoutingDecision(
                    firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), "PJB_PADRAO"),
                    firstNonBlank(processo.getTribunal(), "PJB"),
                    fallbackSystem,
                    fallbackCapability,
                    processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                    false,
                    false,
                    List.of("ROUTING_DECISION_FALLBACK"),
                    Map.of(),
                    Instant.now()
            );
        }
        TribunalProtocolRoutingService.RoutingDecision resolvedRoutingDecision = routingDecision;
        JudicialSystem currentConnector = parseSystem(processo.getConnectorSystem()).orElse(null);
        JudicialSystem selectedSystem = currentConnector != null ? currentConnector : resolvedRoutingDecision.judicialSystem();
        JudicialProcessConnector selectedConnector = judicialConnectorRegistry.find(selectedSystem)
                .orElseGet(() -> judicialConnectorRegistry.get(resolvedRoutingDecision.judicialSystem()));
        JudicialSubmissionCapability capability = selectedConnector.capability();
        ProtocolSubmissionRequest probe = new ProtocolSubmissionRequest(
                "DIAG-" + processo.getId(),
                processo.getNumeroProcesso(),
                firstNonBlank(processo.getClasseProcessual(), "PROCESSO JUDICIAL"),
                firstNonBlank(routingDecision.tribunalCodigo(), processo.getTribunalCodigoRoteado()),
                processo.getUnidadeJudiciariaCodigo(),
                null,
                processo.getRito() != null ? processo.getRito().name() : null,
                processo.getClasseTpuCodigo(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                "{}",
                null,
                null,
                null,
                true,
                payload
        );
        JudicialConnectorHomologationReport homologation = homologationService.analyze(selectedConnector.system(), capability, probe);
        JudicialConnectorReadinessReport readiness = readinessService.analyze(selectedConnector.system(), capability, probe);
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfileService.analyze(selectedConnector.system(), capability, probe);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routingDecision", routingDecision.metadata());
        metadata.put("connectorCapability", JudicialMapSupport.compact(
                "system", capability.system() != null ? capability.system().name() : null,
                "enabled", capability.enabled(),
                "supportsProtocol", capability.supportsProtocol(),
                "supportsDryRun", capability.supportsDryRun(),
                "supportsSnapshotSync", capability.supportsSnapshotSync(),
                "supportsEventSync", capability.supportsEventSync(),
                "baseUrl", capability.baseUrl()
        ));
        metadata.put("homologation", homologation.toMap());
        metadata.put("readiness", readiness.toMap());
        metadata.put("operationalProfile", operationalProfile.toMap());
        metadata.put("processoStatus", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("connectorProtocolReference", processo.getConnectorProtocolReference());
        metadata.put("connectorSubmissionStatus", processo.getConnectorSubmissionStatus());
        metadata.put("connectorSyncStatus", processo.getConnectorSyncStatus());
        metadata.values().removeIf(Objects::isNull);
        List<String> warnings = new ArrayList<>();
        warnings.addAll(routingDecision.warnings());
        warnings.addAll(homologation.warnings());
        warnings.addAll(readiness.warnings());
        warnings.addAll(operationalProfile.warnings());
        List<ExternalIntegrationDiagnosticResponse.ConnectorOptionView> landscape = request.includeConnectorLandscape()
                ? judicialConnectorRegistry.all().stream().map(this::toView).toList()
                : List.of();
        return new ExternalIntegrationDiagnosticResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                routingDecision.tribunalCodigo(),
                routingDecision.tribunalNome(),
                currentConnector != null ? currentConnector.name() : null,
                selectedConnector.system().name(),
                judicialConnectorRegistry.find(selectedConnector.system()).isPresent(),
                capability.operational(),
                request.includeSubmissionReadiness() && readiness.readyForSubmission(),
                request.includeSubmissionReadiness() && readiness.readyForDryRun(),
                routingDecision.stepUpRequired(),
                routingDecision.certificateRequired(),
                landscape,
                List.copyOf(new java.util.LinkedHashSet<>(warnings)),
                metadata,
                Instant.now()
        );
    }

    private ExternalIntegrationDiagnosticResponse.ConnectorOptionView toView(JudicialProcessConnector connector) {
        JudicialSubmissionCapability capability = connector.capability();
        return new ExternalIntegrationDiagnosticResponse.ConnectorOptionView(
                connector.system().name(),
                capability.enabled(),
                capability.supportsProtocol(),
                capability.supportsDryRun(),
                capability.supportsSnapshotSync(),
                capability.supportsEventSync(),
                capability.operational(),
                capability.baseUrl()
        );
    }

    private Map<String, Object> buildPayload(Processo processo) {
        return JudicialMapSupport.compact(
                "processoId", processo.getId(),
                "numeroProcesso", processo.getNumeroProcesso(),
                "tribunalCodigo", firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()),
                "uf", processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null,
                "comarca", processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : null,
                "classeProcessual", processo.getClasseProcessual(),
                "assunto", processo.getAssunto(),
                "connectorSystem", processo.getConnectorSystem(),
                "unidadeJudiciariaCodigo", processo.getUnidadeJudiciariaCodigo(),
                "ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                "rito", processo.getRito() != null ? processo.getRito().name() : null
        );
    }

    private boolean isRecursal(Processo processo) {
        return processo.getStatusProcesso() == StatusProcesso.RECURSO_INTERPOSTO
                || (processo.getFaseAtual() != null && processo.getFaseAtual().name().contains("RECURSAL"));
    }

    private Optional<JudicialSystem> parseSystem(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Optional.of(JudicialSystem.valueOf(normalized));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
