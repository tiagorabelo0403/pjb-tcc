package com.tcc.pjb.backend.service.processual.resilience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.processual.resilience.NationalContingencyAssessmentRequest;
import com.tcc.pjb.backend.model.dto.processual.resilience.NationalContingencyAssessmentResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class NationalContingencyOrchestratorService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final TribunalProtocolRoutingService routingService;
    private final JudicialConnectorRegistry connectorRegistry;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public NationalContingencyOrchestratorService(ProcessoRepository processoRepository,
                                                  WorkItemRepository workItemRepository,
                                                  PjbAuthorizationService authorizationService,
                                                  TribunalProtocolRoutingService routingService,
                                                  JudicialConnectorRegistry connectorRegistry,
                                                  JudicialConnectorOperationalProfileService operationalProfileService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.routingService = Objects.requireNonNull(routingService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public NationalContingencyAssessmentResponse assess(NationalContingencyAssessmentRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        JudicialSystem system = request.judicialSystem() != null ? request.judicialSystem() : resolveSystem(processo);
        TribunalProtocolRoutingService.RoutingDecision routing = routingService.resolve(
                buildPayload(processo),
                processo.getRito(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                processo.getFaseAtual() != null && processo.getFaseAtual().name().contains("RECURSAL")
        );
        if (routing == null) {
            JudicialSubmissionCapability fallbackCapability = connectorRegistry.find(system)
                    .map(JudicialProcessConnector::capability)
                    .orElse(new JudicialSubmissionCapability(system, true, false, false, false, false, false, false, false, List.of(), List.of(), List.of(), null));
            routing = new TribunalProtocolRoutingService.RoutingDecision(
                    firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), "PJB_PADRAO"),
                    firstNonBlank(processo.getTribunal(), "PJB"),
                    system,
                    fallbackCapability,
                    processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                    false,
                    false,
                    List.of("ROUTING_DECISION_FALLBACK"),
                    Map.of(),
                    java.time.Instant.now()
            );
        }
        TribunalProtocolRoutingService.RoutingDecision routingDecision = routing;
        JudicialProcessConnector connector = connectorRegistry.find(system).orElseGet(() -> connectorRegistry.get(routingDecision.judicialSystem()));
        JudicialSubmissionCapability capability = connector.capability();
        ProtocolSubmissionRequest probe = new ProtocolSubmissionRequest(
                "CONTINGENCY-" + processo.getId(),
                processo.getNumeroProcesso(),
                firstNonBlank(processo.getClasseProcessual(), "PROCESSO JUDICIAL"),
                firstNonBlank(routing.tribunalCodigo(), processo.getTribunal()),
                processo.getUnidadeJudiciariaCodigo(),
                processo.getVara(),
                processo.getRito() != null ? processo.getRito().name() : null,
                processo.getClasseTpuCodigo(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                "{}",
                null,
                null,
                null,
                !request.requireSubmission(),
                buildPayload(processo)
        );
        JudicialConnectorOperationalProfileReport profile = operationalProfileService.analyze(connector.system(), capability, probe);
        long officialOpen = workItemRepository.countOpenBlockingByProcessAndRole(processo.getId(), TipoUsuario.OFICIAL_JUSTICA);
        boolean dryRunOnly = !profile.readyForTribunalSubmission()
                && profile.readiness() != null
                && profile.readiness().readyForDryRun();
        boolean queueRetryRecommended = !profile.readyForTribunalSubmission() && connectorRegistry.find(connector.system()).isPresent();
        boolean officialEscalation = request.forceOficialFallback() || officialOpen > 0L;
        boolean manualFallbackRequired = !profile.readyForTribunalSubmission() && !dryRunOnly;
        String contingencyMode = resolveMode(profile, dryRunOnly, manualFallbackRequired, officialEscalation);
        List<String> actions = new ArrayList<>();
        if ("NORMAL".equals(contingencyMode)) {
            actions.add("Fluxo segue no conector homologado sem contingência extra.");
        } else {
            if (dryRunOnly) {
                actions.add("Executar dry-run institucional e preservar payload para reenvio assíncrono.");
            }
            if (queueRetryRecommended) {
                actions.add("Encaminhar para fila de retry controlado com chave de idempotência do processo.");
            }
            if (manualFallbackRequired) {
                actions.add("Acionar secretaria para contingência manual e retenção auditável do protocolo.");
            }
            if (officialEscalation) {
                actions.add("Escalonar fluxo físico ou híbrido para oficial de justiça quando a comunicação exigir materialidade externa.");
            }
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routingWarnings", routing.warnings());
        metadata.put("connector", connector.system().name());
        metadata.put("readyForProduction", profile.readyForProduction());
        metadata.put("readyForSubmission", profile.readyForTribunalSubmission());
        metadata.put("officialOpenBlocking", officialOpen);
        metadata.put("finalidade", request.finalidade());
        metadata.put("operationalProfile", profile.toMap());
        metadata.values().removeIf(Objects::isNull);
        return new NationalContingencyAssessmentResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                connector.system().name(),
                contingencyMode,
                "NORMAL".equals(contingencyMode),
                manualFallbackRequired,
                queueRetryRecommended,
                dryRunOnly,
                officialEscalation,
                profile.blockers(),
                profile.warnings(),
                List.copyOf(actions),
                Collections.unmodifiableMap(metadata)
        );
    }

    private String resolveMode(JudicialConnectorOperationalProfileReport profile,
                               boolean dryRunOnly,
                               boolean manualFallbackRequired,
                               boolean officialEscalation) {
        if (profile.readyForProduction()) {
            return "NORMAL";
        }
        if (dryRunOnly) {
            return "DRY_RUN_CONTINGENCY";
        }
        if (officialEscalation) {
            return "PHYSICAL_OR_HYBRID_FALLBACK";
        }
        if (manualFallbackRequired) {
            return "SECRETARIAT_CONTINGENCY";
        }
        return "RETRY_QUEUE";
    }

    private JudicialSystem resolveSystem(Processo processo) {
        String raw = processo.getConnectorSystem();
        if (raw != null && !raw.isBlank()) {
            try {
                return JudicialSystem.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
            } catch (Exception ignored) {
            }
        }
        return JudicialSystem.PJE;
    }

    private Map<String, Object> buildPayload(Processo processo) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processo.getId());
        payload.put("numeroProcesso", processo.getNumeroProcesso());
        payload.put("tribunalCodigo", processo.getTribunal());
        payload.put("uf", processo.getUf());
        payload.put("comarca", processo.getComarca());
        payload.put("classeProcessual", processo.getClasseProcessual());
        payload.put("assunto", processo.getAssunto());
        payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        payload.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
        payload.values().removeIf(Objects::isNull);
        return Collections.unmodifiableMap(payload);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
