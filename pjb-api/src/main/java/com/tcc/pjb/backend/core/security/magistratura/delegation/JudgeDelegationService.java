package com.tcc.pjb.backend.core.security.magistratura.delegation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@Service
public class JudgeDelegationService {

    private static final int MAX_MINUTES = 24 * 60; 
    private static final int MIN_MINUTES = 1;

    private final UsuarioRepository usuarioRepository;
    private final DelegationTokenService tokenService;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;

    public JudgeDelegationService(UsuarioRepository usuarioRepository,
                                  DelegationTokenService tokenService,
                                  CurrentUserService currentUserService,
                                  AuditLedgerService auditLedgerService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.currentUserService = currentUserService;
        this.auditLedgerService = auditLedgerService;
    }

    
    public String emitirDelegacaoParaAssessor(Long assessorId,
                                              int duracaoMinutos,
                                              DelegationScope scope,
                                              String deviceBindingHash) {
        return emitirDelegacaoParaAssessorDetalhado(assessorId, duracaoMinutos, scope, deviceBindingHash).token();
    }

    public IssueResult emitirDelegacaoParaAssessorDetalhado(Long assessorId,
                                                   int duracaoMinutos,
                                                   DelegationScope scope,
                                                   String deviceBindingHash) {
        Objects.requireNonNull(assessorId, "assessorId é obrigatório");
        if (duracaoMinutos < MIN_MINUTES || duracaoMinutos > MAX_MINUTES) {
            throw new IllegalArgumentException("duracaoMinutos deve estar entre 1 e 1440");
        }

        Usuario juiz = currentUserService.getRequired();
        if (!juiz.isMagistrado()) {
            throw new SecurityException("Apenas magistrados podem emitir delegação.");
        }

        Usuario assessor = usuarioRepository.findById(assessorId)
                .orElseThrow(() -> new SecurityException("Destinatário não encontrado"));

        if (!assessor.isAtivo()) {
            throw new SecurityException("Destinatário inativo não pode receber delegação.");
        }

        
        TipoUsuario emissorTipo = juiz.getTipoUsuario();
        TipoUsuario destinatarioTipo = assessor.getTipoUsuario();

        if (emissorTipo == TipoUsuario.MINISTRO) {
            
            if (destinatarioTipo != TipoUsuario.JUIZ) {
                throw new SecurityException("Violação de Protocolo: ministro só pode delegar para JUIZ (juiz auxiliar).");
            }
        } else {
            
            if (destinatarioTipo != TipoUsuario.SERVIDOR && destinatarioTipo != TipoUsuario.SERVIDOR_FORUM) {
                throw new SecurityException("Violação de Protocolo: apenas SERVIDORES podem receber delegação de magistrado.");
            }
        }

        
        String juizUf = normalize(juiz.getUf());
        String juizComarca = normalize(juiz.getComarca());
        String assUf = normalize(assessor.getUf());
        String assComarca = normalize(assessor.getComarca());

        if (juizUf == null || juizComarca == null || assUf == null || assComarca == null) {
            throw new SecurityException("Perímetro indefinido (UF/Comarca). Configure UF e comarca para emissor e destinatário.");
        }
        if (!juizUf.equals(assUf) || !juizComarca.equals(assComarca)) {
            throw new SecurityException("Violação de Perímetro: destinatário de outra comarca/unidade não pode receber delegação.");
        }

        DelegationScope effectiveScope = (scope != null) ? scope : DelegationScope.READ_WRITE_DRAFT;

        long now = Instant.now().getEpochSecond();
        long exp = now + duracaoMinutos * 60L;

        DelegationTokenPayload payload = new DelegationTokenPayload(
                UUID.randomUUID().toString(),
                juiz.getId(),
                assessor.getId(),
                juizUf,
                juizComarca,
                normalize(deviceBindingHash),
                now,
                exp,
                effectiveScope.name()
        );

        String token = tokenService.sign(payload);

        
        auditLedgerService.appendSafely(
                "DELEGATION_ISSUED",
                "DCP",
                payload.jti(),
                "magistrate=" + juiz.getId() + " delegate=" + assessor.getId() + " scope=" + effectiveScope.name() + " exp=" + exp
        );

        return new IssueResult(token, payload, Instant.ofEpochSecond(exp));
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isBlank() ? null : s;
    }

    public record IssueResult(String token, DelegationTokenPayload payload, Instant expiresAt) {
    }
}
