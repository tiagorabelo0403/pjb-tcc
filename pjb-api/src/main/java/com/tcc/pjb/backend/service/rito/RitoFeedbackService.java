package com.tcc.pjb.backend.service.rito;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.admin.RitoFeedbackRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoFeedbackResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.RitoFeedback;
import com.tcc.pjb.backend.model.entity.RitoOverride;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RitoFeedbackRepository;
import com.tcc.pjb.backend.model.repository.RitoOverrideRepository;

@Service
public class RitoFeedbackService {

    private final ProcessoRepository processoRepository;
    private final RitoFeedbackRepository feedbackRepository;
    private final RitoOverrideRepository overrideRepository;
    private final RitoResolutionService ritoResolutionService;
    private final RitoPackService ritoPackService;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;
    private final RitoMetrics ritoMetrics;

    public RitoFeedbackService(ProcessoRepository processoRepository,
                              RitoFeedbackRepository feedbackRepository,
                              RitoOverrideRepository overrideRepository,
                              RitoResolutionService ritoResolutionService,
                              RitoPackService ritoPackService,
                              CurrentUserService currentUserService,
                              AuditLedgerService auditLedgerService,
                              RitoMetrics ritoMetrics) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.feedbackRepository = Objects.requireNonNull(feedbackRepository);
        this.overrideRepository = Objects.requireNonNull(overrideRepository);
        this.ritoResolutionService = Objects.requireNonNull(ritoResolutionService);
        this.ritoPackService = Objects.requireNonNull(ritoPackService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.ritoMetrics = Objects.requireNonNull(ritoMetrics);
    }

    @Transactional
    public RitoFeedbackResponse registerFeedback(RitoFeedbackRequest req) {
        Objects.requireNonNull(req);

        Processo p = processoRepository.findById(req.processoId()).orElseThrow(() ->
                new IllegalArgumentException("Processo não encontrado: " + req.processoId()));

        
        RitoProcessual chosen = parseRito(req.ritoChosen());
        if (chosen == null) {
            throw new IllegalArgumentException("ritoChosen inválido: " + req.ritoChosen());
        }
        
        ritoPackService.get(chosen); 

        var rr = ritoResolutionService.resolve(p, null);
        List<String> reasons = rr != null ? rr.reasons() : List.of();

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;

        RitoFeedback fb = new RitoFeedback();
        fb.setId(UUID.randomUUID());
        fb.setProcessoId(p.getId());
        fb.setRitoResolved(rr != null && rr.rito() != null ? rr.rito().name() : null);
        fb.setRitoChosen(chosen.name());
        fb.setConfidence(rr != null ? rr.confidence() : null);
        fb.setReasonsJson(toJsonArray(reasons));
        fb.setNotes(req.notes());
        fb.setCreatedAt(OffsetDateTime.now());
        fb.setCreatedByUserId(actorId);
        feedbackRepository.save(fb);

        boolean applyOverride = Boolean.TRUE.equals(req.applyOverride());
        ritoMetrics.incFeedbackRegistered(fb.getRitoResolved(), fb.getRitoChosen(), applyOverride);
        if (applyOverride) {
            upsertOverride(p.getId(), chosen.name(), actorId);
            ritoResolutionService.invalidateCache(p.getId());
            ritoMetrics.incOverrideApplied();
        }

        
        String payloadHash = sha256("processoId=" + p.getId() + "|ritoChosen=" + chosen.name() + "|override=" + applyOverride);
        auditLedgerService.appendSafely(
                applyOverride ? "RITO_FEEDBACK_OVERRIDE" : "RITO_FEEDBACK",
                "PROCESSO",
                String.valueOf(p.getId()),
                payloadHash,
                req.notes()
        );

        return new RitoFeedbackResponse(
                fb.getId(),
                p.getId(),
                fb.getRitoResolved(),
                fb.getRitoChosen(),
                fb.getConfidence(),
                applyOverride,
                fb.getCreatedAt(),
                reasons
        );
    }

    private void upsertOverride(Long processoId, String ritoCode, Long actorId) {
        RitoOverride ov = overrideRepository.findByProcessoId(processoId).orElseGet(RitoOverride::new);
        ov.setProcessoId(processoId);
        ov.setRitoCode(ritoCode);
        ov.setUpdatedAt(OffsetDateTime.now());
        ov.setUpdatedByUserId(actorId);
        overrideRepository.save(ov);
    }

    private static RitoProcessual parseRito(String raw) {
        return raw == null || raw.isBlank() ? null : RitoProcessual.tryParse(raw).orElse(null);
    }

    private static String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            String it = items.get(i);
            sb.append('"').append(it.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            if (i < items.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
