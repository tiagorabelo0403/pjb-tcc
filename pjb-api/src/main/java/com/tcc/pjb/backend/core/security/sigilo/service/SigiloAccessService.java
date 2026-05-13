package com.tcc.pjb.backend.core.security.sigilo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SigiloAccessService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int PASSWORD_BYTES = 12;

    private final SigiloAccessRequestRepository repository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLedgerService auditLedgerService;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public SigiloAccessRequest criarSolicitacao(Long processoId, String motivo) {
        Usuario actor = currentUserService.get();
        if (actor.getTipoUsuario() != TipoUsuario.ADVOGADO) {
            throw new SecurityException("Apenas ADVOGADO pode solicitar acesso a sigilo.");
        }
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório.");
        }

        SigiloAccessRequest req = SigiloAccessRequest.builder()
                .processoId(processoId)
                .advogadoId(actor.getId())
                .motivo(motivo)
                .status(SigiloAccessStatus.PENDENTE)
                .requestedAt(LocalDateTime.now())
                .hideApprover(true)
                .build();

        SigiloAccessRequest saved = repository.save(req);
        auditLedgerService.appendSafely("SIGILO_ACCESS_REQUESTED", "SIGILO_ACCESS", saved.getId().toString(),
                "{\"processoId\":" + processoId + "}");
        return saved;
    }

    @Transactional
    public AprovarResult aprovarSolicitacao(UUID requestId) {
        Usuario approver = currentUserService.get();
        if (!isPerfilInstitucional(approver)) {
            throw new SecurityException("Somente perfis institucionais podem aprovar solicitações de sigilo.");
        }
        SigiloAccessRequest req = repository.findById(requestId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SigiloAccessRequest", requestId));

        LocalDateTime now = LocalDateTime.now();
        if (req.getStatus() != SigiloAccessStatus.PENDENTE) {
            throw new IllegalArgumentException("Solicitação não está pendente.");
        }

        String plain = gerarSenha();
        req.setPasswordHash(passwordEncoder.encode(plain));
        req.setApprovedAt(now);
        req.setApprovedBy(approver.getId());
        req.setExpiresAt(now.plusDays(10));
        req.setStatus(SigiloAccessStatus.APROVADA);
        req.setFailedAttempts(0);
        req.setLockedUntil(null);

        SigiloAccessRequest saved = repository.save(req);
        auditLedgerService.appendSafely("SIGILO_ACCESS_APPROVED", "SIGILO_ACCESS", saved.getId().toString(),
                "{\"processoId\":" + saved.getProcessoId() + "}");

        return new AprovarResult(saved, plain);
    }

    @Transactional
    public SigiloAccessRequest rejeitarSolicitacao(UUID requestId, String motivoRejeicao) {
        Usuario approver = currentUserService.get();
        if (!isPerfilInstitucional(approver)) {
            throw new SecurityException("Somente perfis institucionais podem rejeitar solicitações de sigilo.");
        }

        SigiloAccessRequest req = repository.findById(requestId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SigiloAccessRequest", requestId));

        if (req.getStatus() != SigiloAccessStatus.PENDENTE) {
            throw new IllegalArgumentException("Solicitação não está pendente.");
        }

        req.setStatus(SigiloAccessStatus.REJEITADA);
        req.setRejectedReason(motivoRejeicao);
        SigiloAccessRequest saved = repository.save(req);
        auditLedgerService.appendSafely("SIGILO_ACCESS_REJECTED", "SIGILO_ACCESS", saved.getId().toString(),
                "{\"processoId\":" + saved.getProcessoId() + "}");
        return saved;
    }

    @Transactional
    public SigiloAccessRequest revogarSolicitacao(UUID requestId, String motivo) {
        Usuario actor = currentUserService.get();
        if (!isPerfilInstitucional(actor)) {
            throw new SecurityException("Somente perfis institucionais podem revogar acesso a sigilo.");
        }

        SigiloAccessRequest req = repository.findById(requestId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SigiloAccessRequest", requestId));

        if (req.getStatus() != SigiloAccessStatus.APROVADA) {
            throw new IllegalArgumentException("Apenas solicitações aprovadas podem ser revogadas.");
        }

        req.setStatus(SigiloAccessStatus.REVOGADA);
        req.setRevokedAt(LocalDateTime.now());
        req.setRevokedBy(actor.getId());
        if (motivo != null && !motivo.isBlank()) {
            req.setRejectedReason(motivo);
        }
        SigiloAccessRequest saved = repository.save(req);
        auditLedgerService.appendSafely("SIGILO_ACCESS_REVOKED", "SIGILO_ACCESS", saved.getId().toString(),
                "{\"processoId\":" + saved.getProcessoId() + "}");
        return saved;
    }

    @Transactional
    public boolean validarCredencial(Long processoId, Long advogadoId, SigiloCredential credential) {
        if (processoId == null || advogadoId == null || credential == null || credential.requestId() == null) {
            return false;
        }

        SigiloAccessRequest req = repository.findById(credential.requestId()).orElse(null);
        if (req == null) return false;

        if (!processoId.equals(req.getProcessoId())) return false;
        if (!advogadoId.equals(req.getAdvogadoId())) return false;

        LocalDateTime now = LocalDateTime.now();

        
        if (req.getStatus() == SigiloAccessStatus.APROVADA && req.getExpiresAt() != null && !now.isBefore(req.getExpiresAt())) {
            req.setStatus(SigiloAccessStatus.EXPIRADA);
            repository.save(req);
            return false;
        }

        if (!req.isApprovedAndActive(now)) return false;
        if (req.isLocked(now)) return false;

        String pwd = credential.password();
        if (pwd == null || pwd.isBlank()) return false;

        boolean ok = req.getPasswordHash() != null && passwordEncoder.matches(pwd, req.getPasswordHash());
        if (!ok) {
            int attempts = req.getFailedAttempts() + 1;
            req.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                req.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            }
            repository.save(req);
            return false;
        }

        
        req.setFailedAttempts(0);
        req.setLockedUntil(null);
        req.setLastUsedAt(now);
        repository.save(req);

        auditLedgerService.appendSafely("SIGILO_CREDENTIAL_USED", "SIGILO_ACCESS", req.getId().toString(),
                "{\"processoId\":" + processoId + "}");
        return true;
    }

    @Transactional
    public List<SigiloAccessRequest> listarMinhasSolicitacoes() {
        Usuario actor = currentUserService.get();
        return repository.findByAdvogadoIdOrderByRequestedAtDesc(actor.getId());
    }

    @Transactional
    public SigiloAccessRequest buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SigiloAccessRequest", id));
    }

    public boolean viewerPodeVerAprovador(Usuario viewer) {
        if (viewer == null) return false;
        
        return viewer.getTipoUsuario() == TipoUsuario.ADMINISTRADOR || viewer.getTipoUsuario() == TipoUsuario.SERVIDOR;
    }

    private boolean isPerfilInstitucional(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return false;
        TipoUsuario t = u.getTipoUsuario();
        return t.isMagistratura() || t.isMinisterioPublico() || t.isDefensoriaPublica() || t.isProcuradoria() || t == TipoUsuario.SERVIDOR || t == TipoUsuario.ADMINISTRADOR;
    }

    private String gerarSenha() {
        byte[] bytes = new byte[PASSWORD_BYTES];
        random.nextBytes(bytes);
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record AprovarResult(SigiloAccessRequest request, String plainPassword) {
    }
}
