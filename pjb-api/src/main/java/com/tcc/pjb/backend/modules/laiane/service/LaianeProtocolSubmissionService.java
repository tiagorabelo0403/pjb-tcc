package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeDelegationService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeDelegationService.Decision;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage;
import com.tcc.pjb.backend.modules.laiane.jobs.LaianeProtocolSubmitJobInput;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProtocolPackageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LaianeProtocolSubmissionService {

    public static final String RESOURCE_TYPE = "LAIANE_PROTOCOL";

    private final CurrentUserService currentUserService;
    private final LaianeProtocolPackageRepository protocolRepo;
    private final OfficeDelegationService officeDelegationService;
    private final JobCommandService jobCommandService;
    private final AuditLedgerService auditLedgerService;
    private final LaianeNationalPreflightService nationalPreflightService;
    private final LaianeSubmissionGuardrailService laianeSubmissionGuardrailService;
    private final boolean mockEnabled;

    public LaianeProtocolSubmissionService(CurrentUserService currentUserService,
                                           LaianeProtocolPackageRepository protocolRepo,
                                           OfficeDelegationService officeDelegationService,
                                           JobCommandService jobCommandService,
                                           AuditLedgerService auditLedgerService,
                                           LaianeNationalPreflightService nationalPreflightService,
                                           LaianeSubmissionGuardrailService laianeSubmissionGuardrailService,
                                           @Value("${pjb.integrations.pje.mock-enabled:${PJB_INTEGRATIONS_PJE_MOCK_ENABLED:false}}") boolean mockEnabled) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.protocolRepo = Objects.requireNonNull(protocolRepo);
        this.officeDelegationService = Objects.requireNonNull(officeDelegationService);
        this.jobCommandService = Objects.requireNonNull(jobCommandService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.nationalPreflightService = Objects.requireNonNull(nationalPreflightService);
        this.laianeSubmissionGuardrailService = Objects.requireNonNull(laianeSubmissionGuardrailService);
        this.mockEnabled = mockEnabled;
    }

    @Transactional
    public LaianeProtocolPackageDto submit(Long protocolId) {
        Usuario u = currentUserService.get();
        LaianeProtocolPackage p = protocolRepo.findById(protocolId)
                .orElseThrow(() -> new EntityNotFoundException("Protocolo não encontrado."));

        if (p.getUsuario() == null || p.getUsuario().getId() == null || !p.getUsuario().getId().equals(u.getId())) {
            throw new EntityNotFoundException("Protocolo não encontrado.");
        }
        if (isFinal(p)) {
            return toDto(p);
        }
        if ("PENDING_SIGNER".equalsIgnoreCase(p.getStatus()) && p.getOfficeQueueItemId() != null) {
            return toDto(p);
        }
        if ("SUBMISSION_QUEUED".equalsIgnoreCase(p.getStatus()) && p.getSubmissionJobId() != null) {
            return toDto(p);
        }

        LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails = laianeSubmissionGuardrailService.analyze(p.getPayloadJson());
        if (guardrails.blocking() || !guardrails.readyForSubmission()) {
            p.setStatus("PRECHECK_PENDING");
            p.setLastError(guardrails.summary());
            p.setSubmissionJobId(null);
            protocolRepo.save(p);
            auditLedgerService.appendSafely("ADV_PROTOCOL_GUARDRAIL_BLOCKED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), p.getLastError());
            return toDto(p);
        }

        LaianeNationalPreflightService.PreflightResult preflight = nationalPreflightService.analyze(p.getPayloadJson());
        if (preflight == null || !preflight.readyForSubmission()) {
            p.setStatus("PRECHECK_PENDING");
            p.setLastError(summarizeIssues(preflight));
            p.setSubmissionJobId(null);
            protocolRepo.save(p);
            auditLedgerService.appendSafely("ADV_PROTOCOL_PRECHECK_BLOCKED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), p.getLastError());
            return toDto(p);
        }

        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        Long equipeId = membro != null && membro.getEquipe() != null ? membro.getEquipe().getId() : null;
        Long executorId = u.getId();
        Long signerId;
        Long queueItemId = null;
        OfficeDelegationMode mode = OfficeDelegationMode.SELF;

        if (equipeId != null) {
            Decision d = officeDelegationService.decideAndRecord(
                    equipeId,
                    executorId,
                    OfficeActionType.PROTOCOL_SUBMIT_PJE,
                    RESOURCE_TYPE,
                    String.valueOf(p.getId()),
                    p.getIntegrityHash(),
                    safeSummary(p)
            );
            signerId = d.signerUserId();
            mode = d.mode();
            queueItemId = d.queueItemId();
        } else {
            signerId = executorId;
        }

        p.setEquipe(membro != null ? membro.getEquipe() : null);
        p.setExecutorUserId(executorId);
        p.setSignerUserId(signerId);
        p.setLastError(null);

        if (mode == OfficeDelegationMode.QUEUE) {
            p.setStatus("PENDING_SIGNER");
            p.setOfficeQueueItemId(queueItemId);
            p.setSubmissionJobId(null);
            protocolRepo.save(p);
            auditLedgerService.appendSafely("ADV_PROTOCOL_SUBMIT_QUEUED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash());
            return toDto(p);
        }

        UUID jobId = enqueueSubmitJob(p, equipeId, executorId, signerId, null);
        p.setOfficeQueueItemId(null);
        p.setSubmissionJobId(jobId);
        p.setStatus("SUBMISSION_QUEUED");
        protocolRepo.save(p);
        auditLedgerService.appendSafely("ADV_PROTOCOL_SUBMIT_JOB", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash());
        return toDto(p);
    }

    @Transactional
    public LaianeProtocolPackageDto enqueueFromQueue(Long protocolId,
                                                     Long equipeId,
                                                     Long executorUserId,
                                                     Long signerUserId,
                                                     Long queueItemId,
                                                     Long decidedByUserId,
                                                     String reason) {
        LaianeProtocolPackage p = protocolRepo.findById(protocolId)
                .orElseThrow(() -> new EntityNotFoundException("Protocolo não encontrado."));
        if (isFinal(p)) {
            return toDto(p);
        }
        LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails = laianeSubmissionGuardrailService.analyze(p.getPayloadJson());
        if (guardrails.blocking() || !guardrails.readyForSubmission()) {
            p.setStatus("PRECHECK_PENDING");
            p.setLastError(guardrails.summary());
            protocolRepo.save(p);
            auditLedgerService.appendSafely("ADV_PROTOCOL_QUEUE_GUARDRAIL_BLOCKED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), p.getLastError());
            return toDto(p);
        }
        LaianeNationalPreflightService.PreflightResult preflight = nationalPreflightService.analyze(p.getPayloadJson());
        if (preflight == null || !preflight.readyForSubmission()) {
            p.setStatus("PRECHECK_PENDING");
            p.setLastError(summarizeIssues(preflight));
            protocolRepo.save(p);
            auditLedgerService.appendSafely("ADV_PROTOCOL_QUEUE_PRECHECK_BLOCKED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), p.getLastError());
            return toDto(p);
        }
        if (p.getSubmissionJobId() != null && "SUBMISSION_QUEUED".equalsIgnoreCase(p.getStatus())) {
            return toDto(p);
        }

        p.setExecutorUserId(executorUserId);
        p.setSignerUserId(signerUserId);
        p.setOfficeQueueItemId(queueItemId);
        p.setLastError(null);

        UUID jobId = enqueueSubmitJob(p, equipeId, executorUserId, signerUserId, queueItemId);
        p.setSubmissionJobId(jobId);
        p.setStatus("SUBMISSION_QUEUED");
        protocolRepo.save(p);
        auditLedgerService.appendSafely("ADV_PROTOCOL_QUEUE_APPROVED_JOB", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), safeReason(reason));
        return toDto(p);
    }

    @Transactional
    public LaianeProtocolPackageDto rejectFromQueue(Long protocolId,
                                                    Long signerUserId,
                                                    Long queueItemId,
                                                    Long decidedByUserId,
                                                    String reason) {
        LaianeProtocolPackage p = protocolRepo.findById(protocolId)
                .orElseThrow(() -> new EntityNotFoundException("Protocolo não encontrado."));
        if (isFinal(p)) {
            return toDto(p);
        }

        p.setSignerUserId(signerUserId);
        p.setOfficeQueueItemId(queueItemId);
        p.setStatus("REJECTED_BY_SIGNER");
        p.setLastError(safeReason(reason));
        protocolRepo.save(p);
        auditLedgerService.appendSafely("ADV_PROTOCOL_QUEUE_REJECTED", RESOURCE_TYPE, String.valueOf(p.getId()), p.getIntegrityHash(), safeReason(reason));
        return toDto(p);
    }

    private UUID enqueueSubmitJob(LaianeProtocolPackage p,
                                  Long equipeId,
                                  Long executorUserId,
                                  Long signerUserId,
                                  Long queueItemId) {
        String idem = buildIdempotencyKey(p.getId(), p.getIntegrityHash(), signerUserId);
        String inboxKey = "laiane:protocol:submit";
        String owner = signerUserId == null ? null : String.valueOf(signerUserId);

        LaianeProtocolSubmitJobInput input = new LaianeProtocolSubmitJobInput(
                p.getId(),
                equipeId,
                executorUserId,
                signerUserId,
                queueItemId,
                p.getIntegrityHash()
        );

        JobCommandService.JobCreateResult res = jobCommandService.createIdempotent(
                JobType.LAIANE_PROTOCOL_SUBMIT_PJE,
                inboxKey,
                owner,
                idem,
                input,
                0,
                3
        );
        return res.jobId();
    }

    private boolean isFinal(LaianeProtocolPackage p) {
        String st = p.getStatus();
        return "SUBMITTED".equalsIgnoreCase(st);
    }

    private String buildIdempotencyKey(Long protocolId, String integrityHash, Long signerId) {
        String raw = "LAIANE_PROTOCOL_SUBMIT_PJE:" + protocolId + ':' + (integrityHash == null ? "" : integrityHash) + ':' + (signerId == null ? "" : signerId);
        if (raw.length() <= 180) {
            return raw;
        }
        String h = com.tcc.pjb.backend.modules.laiane.util.LaianeCrypto.sha256Hex(raw);
        return "LAIANE_PROTOCOL_SUBMIT_PJE:" + protocolId + ':' + h;
    }

    private LaianeProtocolPackageDto toDto(LaianeProtocolPackage p) {
        LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails = laianeSubmissionGuardrailService.analyze(p.getPayloadJson());
        return LaianeProtocolPackageDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .integrityHash(p.getIntegrityHash())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .equipeId(p.getEquipe() != null ? p.getEquipe().getId() : null)
                .executorUserId(p.getExecutorUserId())
                .signerUserId(p.getSignerUserId())
                .officeQueueItemId(p.getOfficeQueueItemId())
                .submissionJobId(p.getSubmissionJobId())
                .externalProtocolRef(p.getExternalProtocolRef())
                .submittedAt(p.getSubmittedAt())
                .lastError(p.getLastError())
                .guardrailStatus(guardrails.status())
                .readyForSubmission(guardrails.readyForSubmission())
                .guardrailNextAction(guardrails.nextAction())
                .guardrailBlockers(guardrails.blockers())
                .build();
    }

    private String summarizeIssues(LaianeNationalPreflightService.PreflightResult preflight) {
        if (preflight == null) {
            return "Preflight nacional indisponível.";
        }
        List<String> fragments = preflight.issues().stream()
                .filter(item -> item != null && item.blocking())
                .limit(4)
                .map(item -> item.message())
                .toList();
        if (fragments.isEmpty()) {
            fragments = preflight.issues().stream()
                    .filter(Objects::nonNull)
                    .limit(4)
                    .map(item -> item.message())
                    .toList();
        }
        String summary = String.join(" | ", fragments);
        if (summary.isBlank()) {
            summary = "Pré-check não aprovou o protocolo.";
        }
        return summary.length() > 950 ? summary.substring(0, 950) : summary;
    }

    private String safeSummary(LaianeProtocolPackage p) {
        if (p == null) {
            return null;
        }
        String t = p.getTitle();
        if (t == null) {
            return null;
        }
        t = t.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 240 ? t.substring(0, 240) : t;
    }

    private String safeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String r = reason.trim();
        if (r.isEmpty()) {
            return null;
        }
        return r.length() > 240 ? r.substring(0, 240) : r;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }
}
