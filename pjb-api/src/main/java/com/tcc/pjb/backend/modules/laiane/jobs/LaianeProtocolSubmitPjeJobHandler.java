package com.tcc.pjb.backend.modules.laiane.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;
import com.tcc.pjb.backend.core.jobs.runtime.JobPauseException;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService.RoutingDecision;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProtocolPackageRepository;
import com.tcc.pjb.backend.modules.laiane.service.LaianeNationalPreflightService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeProtocolSubmissionService;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LaianeProtocolSubmitPjeJobHandler implements JobHandler {

    private final LaianeProtocolPackageRepository protocolRepo;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper;
    private final LaianeNationalPreflightService nationalPreflightService;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final JudicialConnectorRegistry connectorRegistry;
    private final JudicialConnectorReadinessService judicialConnectorReadinessService;
    private final ProceduralCatalogService proceduralCatalogService;
    private final boolean mockEnabled;

    public LaianeProtocolSubmitPjeJobHandler(LaianeProtocolPackageRepository protocolRepo,
                                             AuditLedgerService auditLedgerService,
                                             ObjectMapper objectMapper,
                                             LaianeNationalPreflightService nationalPreflightService,
                                             TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                             JudicialConnectorRegistry connectorRegistry,
                                             JudicialConnectorReadinessService judicialConnectorReadinessService,
                                             ProceduralCatalogService proceduralCatalogService,
                                             @Value("${pjb.integrations.pje.mock-enabled:${PJB_INTEGRATIONS_PJE_MOCK_ENABLED:false}}") boolean mockEnabled) {
        this.protocolRepo = Objects.requireNonNull(protocolRepo);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.nationalPreflightService = Objects.requireNonNull(nationalPreflightService);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.judicialConnectorReadinessService = Objects.requireNonNull(judicialConnectorReadinessService);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.mockEnabled = mockEnabled;
    }

    @Override
    public JobType type() {
        return JobType.LAIANE_PROTOCOL_SUBMIT_PJE;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext ctx) {
        LaianeProtocolSubmitJobInput input = ctx.inputAs(LaianeProtocolSubmitJobInput.class);
        Long protocolId = input.protocolId();
        if (protocolId == null) {
            throw new JobPauseException("protocolId ausente");
        }

        LaianeProtocolPackage p = protocolRepo.findById(protocolId)
                .orElseThrow(() -> new JobPauseException("Protocolo não encontrado."));
        if (p.getIntegrityHash() != null && input.integrityHash() != null && !p.getIntegrityHash().equalsIgnoreCase(input.integrityHash())) {
            p.setStatus("FAILED");
            p.setLastError("Integridade divergente; re-gerar pacote.");
            protocolRepo.save(p);
            throw new JobPauseException("Integridade divergente");
        }
        if ("SUBMITTED".equalsIgnoreCase(p.getStatus()) && p.getExternalProtocolRef() != null) {
            ctx.progress(1, 1);
            return;
        }

        Map<String, Object> payload = payloadMap(p.getPayloadJson());
        LaianeNationalPreflightService.PreflightResult preflight = nationalPreflightService.analyze(payload);
        if (preflight == null || !preflight.readyForSubmission()) {
            p.setStatus("FAILED");
            p.setLastError(preflight == null ? "Preflight indisponível." : summarizePreflight(preflight));
            protocolRepo.save(p);
            throw new JobPauseException(p.getLastError());
        }

        String ritoName = proceduralCatalogService.resolveRitoName(preflight.rito(), preflight.ramoDireito(), stringValue(payload.get("classeTpu")));
        RoutingDecision routing = tribunalProtocolRoutingService.resolve(payload, ritoName, preflight.ramoDireito(), preflight.competencia(), false);
        JudicialProcessConnector connector = connectorRegistry.get(routing.judicialSystem());
        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                UUID.randomUUID().toString(),
                stringValue(payload.get("numeroUnificado")),
                stringValue(payload.get("title")) != null ? stringValue(payload.get("title")) : p.getTitle(),
                routing.tribunalCodigo(),
                preflight.unidadeJudiciariaCodigo(),
                preflight.unidadeJudiciariaCodigo(),
                ritoName,
                stringValue(payload.get("classeTpu")),
                preflight.ramoDireito(),
                p.getPayloadJson(),
                p.getIntegrityHash(),
                input.signerUserId(),
                input.executorUserId(),
                false,
                JudicialMapSupport.compact(
                        "protocolId", p.getId(),
                        "competencia", preflight.competencia(),
                        "connectorSystem", routing.judicialSystem().name()
                )
        );

        JudicialConnectorReadinessReport readiness = judicialConnectorReadinessService.analyze(routing.judicialSystem(), routing.capability(), request);
        if (!readiness.readyForSubmission()) {
            p.setStatus("WAITING_CONNECTOR_READINESS");
            p.setLastError(readiness.blockers().isEmpty() ? "Conector judicial sem prontidão operacional." : String.join(" | ", readiness.blockers()));
            protocolRepo.save(p);
            throw new JobPauseException(p.getLastError());
        }

        ProtocolSubmissionResult result = connector.submit(request);
        if (!result.accepted()) {
            p.setStatus(mockEnabled ? "WAITING_EXTERNAL_RETRY" : "FAILED");
            p.setLastError(result.message());
            protocolRepo.save(p);
            throw new JobPauseException(result.message());
        }

        p.setExternalProtocolRef(result.protocolReference());
        p.setSubmittedAt(LocalDateTime.now());
        p.setLastError(null);
        p.setStatus("SUBMITTED");
        protocolRepo.save(p);
        auditLedgerService.appendSafely("ADV_PROTOCOL_SUBMITTED", LaianeProtocolSubmissionService.RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), routing.judicialSystem().name());
        ctx.progress(1, 1);
    }

    private Map<String, Object> payloadMap(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(payloadJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new JobPauseException("Payload do protocolo está inválido.");
        }
    }

    private String summarizePreflight(LaianeNationalPreflightService.PreflightResult preflight) {
        StringBuilder sb = new StringBuilder();
        preflight.issues().stream()
                .filter(item -> item != null)
                .limit(4)
                .forEach(item -> {
                    if (sb.length() > 0) {
                        sb.append(" | ");
                    }
                    sb.append(item.message());
                });
        String out = sb.toString();
        return out.isBlank() ? "Pré-check bloqueou o protocolo." : out;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
