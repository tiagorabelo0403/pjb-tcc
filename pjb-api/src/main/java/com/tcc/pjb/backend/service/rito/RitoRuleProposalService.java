package com.tcc.pjb.backend.service.rito;




import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalCreateRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalDecisionRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalDto;
import com.tcc.pjb.backend.model.entity.RitoRuleProposal;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoRuleProposalStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.RitoRuleProposalRepository;

@Service
public class RitoRuleProposalService {

    private final RitoRuleProposalRepository repo;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedger;
    private final RitoMetrics metrics;
    private final RitoPackService ritoPackService;
    private final RitoGovernanceProperties governance;

    public RitoRuleProposalService(RitoRuleProposalRepository repo,
                                  CurrentUserService currentUserService,
                                  AuditLedgerService auditLedger,
                                  RitoMetrics metrics,
                                  RitoPackService ritoPackService,
                                  RitoGovernanceProperties governance) {
        this.repo = Objects.requireNonNull(repo);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.metrics = Objects.requireNonNull(metrics);
        this.ritoPackService = Objects.requireNonNull(ritoPackService);
        this.governance = Objects.requireNonNull(governance);
    }

    @Transactional
    public RitoRuleProposalDto create(RitoRuleProposalCreateRequest req) {
        Objects.requireNonNull(req);

        String resolved = normalizeRito(req.ritoResolved());
        String chosen = normalizeRito(req.ritoChosen());

        if (resolved == null || chosen == null) {
            throw new IllegalArgumentException("ritoResolved/ritoChosen inválidos");
        }

        
        var existing = repo.findByRitoResolvedAndRitoChosenAndStatus(resolved, chosen, RitoRuleProposalStatus.DRAFT);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;

        RitoRuleProposal p = new RitoRuleProposal();
        p.setId(UUID.randomUUID());
        p.setRitoResolved(resolved);
        p.setRitoChosen(chosen);
        p.setOccurrences(req.occurrences() != null ? Math.max(0, req.occurrences()) : 0);
        p.setSampleReasonsJson(blankToNull(req.sampleReasonsJson()));
        p.setRequiresDualApproval(isSensitive(resolved, chosen, p.getOccurrences()));
        p.setStatus(RitoRuleProposalStatus.DRAFT);
        p.setNotes(blankToNull(req.notes()));
        p.setCreatedAt(OffsetDateTime.now());
        p.setCreatedByUserId(actorId);
        repo.save(p);

        metrics.incRuleProposalCreated(resolved, chosen);

        auditLedger.appendSafely(
                "RITO_RULE_PROPOSAL_CREATED",
                "RITO_RULE_PROPOSAL",
                p.getId().toString(),
                sha256("resolved=" + resolved + "|chosen=" + chosen + "|occ=" + p.getOccurrences()),
                p.getNotes()
        );

        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<RitoRuleProposalDto> list(RitoRuleProposalStatus status, int top) {
        int limit = Math.min(Math.max(1, top), 200);
        return repo.findRecent(status, null, PageRequest.of(0, limit)).stream().map(this::toDto).toList();
    }

    @Transactional
    public RitoRuleProposalDto approve(UUID id, RitoRuleProposalDecisionRequest req) {
        Objects.requireNonNull(id);

        if (req == null || !StringUtils.hasText(req.reason())) {
            throw new IllegalArgumentException("Motivo é obrigatório");
        }

        RitoRuleProposal p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada"));
        if (p.getStatus() == RitoRuleProposalStatus.APPROVED) return toDto(p);
        if (p.getStatus() == RitoRuleProposalStatus.REJECTED) {
            throw new IllegalStateException("Proposta já rejeitada");
        }

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;

        String reason = blankToNull(req.reason());

        if (!p.isRequiresDualApproval()) {
            
            p.setStatus(RitoRuleProposalStatus.APPROVED);
            p.setReviewedAt(OffsetDateTime.now());
            p.setReviewedByUserId(actorId);
            p.setDecisionNotes(reason);
            repo.save(p);

            metrics.incRuleProposalApproved(p.getRitoResolved(), p.getRitoChosen());
            auditLedger.appendSafely(
                    "RITO_RULE_PROPOSAL_APPROVED",
                    "RITO_RULE_PROPOSAL",
                    p.getId().toString(),
                    sha256("approved|" + p.getId()),
                    p.getDecisionNotes()
            );
            return toDto(p);
        }

        
        if (p.getStatus() == RitoRuleProposalStatus.DRAFT) {
            p.setStatus(RitoRuleProposalStatus.PENDING_SECOND_APPROVAL);
            p.setFirstReviewedAt(OffsetDateTime.now());
            p.setFirstReviewedByUserId(actorId);
            p.setFirstDecisionNotes(reason);
            repo.save(p);

            metrics.incRuleProposalApprovedStage1(p.getRitoResolved(), p.getRitoChosen());
            auditLedger.appendSafely(
                    "RITO_RULE_PROPOSAL_APPROVED_STAGE1",
                    "RITO_RULE_PROPOSAL",
                    p.getId().toString(),
                    sha256("approved1|" + p.getId()),
                    p.getFirstDecisionNotes()
            );
            return toDto(p);
        }

        if (p.getStatus() == RitoRuleProposalStatus.PENDING_SECOND_APPROVAL) {
            if (p.getFirstReviewedByUserId() != null && Objects.equals(p.getFirstReviewedByUserId(), actorId)) {
                throw new IllegalStateException("Segunda aprovação deve ser feita por outro administrador (4 olhos)");
            }

            p.setStatus(RitoRuleProposalStatus.APPROVED);
            p.setReviewedAt(OffsetDateTime.now());
            p.setReviewedByUserId(actorId);
            p.setSecondDecisionNotes(reason);
            p.setDecisionNotes(consolidate(p.getFirstDecisionNotes(), p.getSecondDecisionNotes()));
            repo.save(p);

            metrics.incRuleProposalApproved(p.getRitoResolved(), p.getRitoChosen());
            metrics.incRuleProposalApprovedStage2(p.getRitoResolved(), p.getRitoChosen());
            auditLedger.appendSafely(
                    "RITO_RULE_PROPOSAL_APPROVED_STAGE2",
                    "RITO_RULE_PROPOSAL",
                    p.getId().toString(),
                    sha256("approved2|" + p.getId()),
                    p.getSecondDecisionNotes()
            );
            return toDto(p);
        }

        throw new IllegalStateException("Status inválido para aprovação: " + p.getStatus());
    }

    @Transactional
    public RitoRuleProposalDto reject(UUID id, RitoRuleProposalDecisionRequest req) {
        Objects.requireNonNull(id);

        if (req == null || !StringUtils.hasText(req.reason())) {
            throw new IllegalArgumentException("Motivo é obrigatório");
        }

        RitoRuleProposal p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada"));
        if (p.getStatus() == RitoRuleProposalStatus.REJECTED) return toDto(p);
        if (p.getStatus() == RitoRuleProposalStatus.APPROVED) {
            throw new IllegalStateException("Proposta já aprovada");
        }

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;

        String reason = blankToNull(req.reason());

        p.setStatus(RitoRuleProposalStatus.REJECTED);
        p.setReviewedAt(OffsetDateTime.now());
        p.setReviewedByUserId(actorId);
        p.setDecisionNotes(reason);
        repo.save(p);

        metrics.incRuleProposalRejected(p.getRitoResolved(), p.getRitoChosen());

        auditLedger.appendSafely(
                "RITO_RULE_PROPOSAL_REJECTED",
                "RITO_RULE_PROPOSAL",
                p.getId().toString(),
                sha256("rejected|" + p.getId()),
                p.getDecisionNotes()
        );

        return toDto(p);
    }

    private RitoRuleProposalDto toDto(RitoRuleProposal p) {
        return new RitoRuleProposalDto(
                p.getId(),
                p.getRitoResolved(),
                p.getRitoChosen(),
                p.getOccurrences(),
                p.getSampleReasonsJson(),
                p.isRequiresDualApproval(),
                p.getStatus(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getCreatedByUserId(),
                p.getFirstReviewedAt(),
                p.getFirstReviewedByUserId(),
                p.getFirstDecisionNotes(),
                p.getReviewedAt(),
                p.getReviewedByUserId(),
                p.getSecondDecisionNotes(),
                p.getDecisionNotes()
        );
    }

    private boolean isSensitive(String ritoResolved, String ritoChosen, Integer occurrences) {
        if (ritoResolved == null || ritoChosen == null) return true;
        if (ritoResolved.equals(ritoChosen)) return false;

        int occ = occurrences != null ? Math.max(0, occurrences) : 0;
        if (occ >= governance.dualApprovalMinOccurrences()) return true;

        String ramoResolved = null;
        String ramoChosen = null;
        try {
            ramoResolved = RitoProcessual.tryParse(ritoResolved).flatMap(ritoPackService::get)
                    .map(d -> d.getRamoSugerido()).orElse(null);
        } catch (Exception ignored) {
        }
        try {
            ramoChosen = RitoProcessual.tryParse(ritoChosen).flatMap(ritoPackService::get)
                    .map(d -> d.getRamoSugerido()).orElse(null);
        } catch (Exception ignored) {
        }

        if (ramoResolved != null && ramoChosen != null && !ramoResolved.equalsIgnoreCase(ramoChosen)) return true;

        if (ramoChosen != null) {
            for (String critical : governance.criticalRamos()) {
                if (critical != null && !critical.isBlank() && ramoChosen.equalsIgnoreCase(critical.trim())) {
                    return true;
                }
            }
        }

        String chosen = ritoChosen.toUpperCase();
        return chosen.contains("PENAL") || chosen.contains("ELEITOR") || chosen.contains("MILITAR") || chosen.contains("INFANCIA");
    }

    private static String consolidate(String a, String b) {
        String x = blankToNull(a);
        String y = blankToNull(b);
        if (x == null) return y;
        if (y == null) return x;
        if (x.length() > 160) x = x.substring(0, 160);
        if (y.length() > 160) y = y.substring(0, 160);
        return "(1) " + x + " | (2) " + y;
    }

    private static String normalizeRito(String raw) {
        return raw == null || raw.isBlank() ? null : RitoProcessual.tryParse(raw).map(Enum::name).orElse(null);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isBlank() ? null : v;
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
