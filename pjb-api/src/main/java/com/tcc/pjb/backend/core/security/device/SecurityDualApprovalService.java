package com.tcc.pjb.backend.core.security.device;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalStatus;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityDualApprovalRequestRepository;

@Service
public class SecurityDualApprovalService {

    private final SecurityDualApprovalRequestRepository repo;
    private final MembroEquipeRepository membroEquipeRepository;

    public SecurityDualApprovalService(SecurityDualApprovalRequestRepository repo,
                                      MembroEquipeRepository membroEquipeRepository) {
        this.repo = Objects.requireNonNull(repo);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
    }

    @Transactional
    public SecurityDualApprovalRequest request(Usuario requester,
                                               Long requesterDeviceId,
                                               Long equipeId,
                                               String action,
                                               String method,
                                               String path,
                                               String ruleId,
                                               String actionHash,
                                               int ttlSeconds) {
        if (requester == null || requester.getId() == null) throw new IllegalArgumentException("requester obrigatório");
        if (actionHash == null || actionHash.isBlank()) throw new IllegalArgumentException("actionHash obrigatório");

        String key = requestKey(requester.getId(), requesterDeviceId, equipeId, actionHash);
        SecurityDualApprovalRequest existing = repo.findByRequestKey(key).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == SecurityDualApprovalStatus.PENDING && !existing.isExpired()) {
                return existing;
            }
            if (existing.getStatus() == SecurityDualApprovalStatus.PENDING && existing.isExpired()) {
                existing.setStatus(SecurityDualApprovalStatus.EXPIRED);
                repo.save(existing);
            }
        }

        SecurityDualApprovalRequest r = new SecurityDualApprovalRequest();
        r.setRequester(requester);
        r.setRequesterDeviceId(requesterDeviceId);
        r.setEquipeId(equipeId);
        r.setAction(safe(action, 40));
        r.setMethod(safe(method, 12));
        r.setPath(safe(path, 300));
        r.setRuleId(safe(ruleId, 64));
        r.setActionHash(actionHash.trim());
        r.setRequestKey(key);
        r.setStatus(SecurityDualApprovalStatus.PENDING);
        r.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(60, ttlSeconds)));

        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public SecurityDualApprovalRequest getRequired(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("approval não encontrado"));
    }

    @Transactional
    public SecurityDualApprovalRequest approve(Long id, Usuario approver) {
        if (approver == null || approver.getId() == null) throw new IllegalArgumentException("approver obrigatório");
        SecurityDualApprovalRequest r = getRequired(id);

        if (r.getStatus() != SecurityDualApprovalStatus.PENDING) throw new IllegalStateException("approval não está pendente");
        if (r.isExpired()) {
            r.setStatus(SecurityDualApprovalStatus.EXPIRED);
            repo.save(r);
            throw new IllegalStateException("approval expirado");
        }
        if (r.getRequester() != null && Objects.equals(r.getRequester().getId(), approver.getId())) {
            throw new IllegalArgumentException("approver não pode ser o solicitante");
        }

        if (r.getEquipeId() != null) {
            boolean ok = membroEquipeRepository.existsByUsuario_IdAndEquipe_Id(approver.getId(), r.getEquipeId());
            if (!ok) throw new IllegalArgumentException("approver não pertence à equipe");
        } else {
            MembroEquipe ctx = RequestContext.getMembroEquipeAtivo().orElse(null);
            if (ctx != null && ctx.getEquipe() != null && ctx.getEquipe().getId() != null) {
                boolean ok = membroEquipeRepository.existsByUsuario_IdAndEquipe_Id(approver.getId(), ctx.getEquipe().getId());
                if (!ok) {
                    throw new IllegalArgumentException("approver sem contexto compatível");
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        r.setStatus(SecurityDualApprovalStatus.APPROVED);
        r.setApprovedBy(approver);
        r.setApprovedAt(now);

        return repo.save(r);
    }

    @Transactional
    public SecurityDualApprovalRequest reject(Long id, Usuario approver) {
        if (approver == null || approver.getId() == null) throw new IllegalArgumentException("approver obrigatório");
        SecurityDualApprovalRequest r = getRequired(id);

        if (r.getStatus() != SecurityDualApprovalStatus.PENDING) throw new IllegalStateException("approval não está pendente");
        if (r.isExpired()) {
            r.setStatus(SecurityDualApprovalStatus.EXPIRED);
            repo.save(r);
            throw new IllegalStateException("approval expirado");
        }

        LocalDateTime now = LocalDateTime.now();
        r.setStatus(SecurityDualApprovalStatus.REJECTED);
        r.setRejectedBy(approver);
        r.setRejectedAt(now);

        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public boolean isApprovedFor(Usuario requester, Long approvalId, String actionHash, Long equipeId) {
        if (requester == null || requester.getId() == null) return false;
        if (approvalId == null) return false;
        if (actionHash == null || actionHash.isBlank()) return false;

        SecurityDualApprovalRequest r = repo.findById(approvalId).orElse(null);
        if (r == null) return false;
        if (r.getStatus() != SecurityDualApprovalStatus.APPROVED) return false;
        if (r.isExpired()) return false;
        if (r.getRequester() == null || !Objects.equals(r.getRequester().getId(), requester.getId())) return false;
        if (!actionHash.trim().equalsIgnoreCase(r.getActionHash())) return false;
        if (!Objects.equals(r.getEquipeId(), equipeId)) return false;
        return true;
    }

    private static String requestKey(Long userId, Long deviceId, Long equipeId, String actionHash) {
        String raw = "u=" + userId + "|d=" + (deviceId != null ? deviceId : "-") + "|e=" + (equipeId != null ? equipeId : "-") + "|a=" + actionHash;
        return sha256Hex(raw);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String safe(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }
}
