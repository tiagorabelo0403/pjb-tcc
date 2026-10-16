package com.tcc.pjb.backend.core.security.device;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescongelamentoContaService {

    static final String ACAO = "ACCOUNT_UNFREEZE";
    private static final int VALIDADE_DA_APROVACAO_SEGUNDOS = 900;

    private final SecurityDualApprovalService dualApprovalService;
    private final UserSecurityProfileRepository profileRepo;
    private final UsuarioRepository usuarioRepository;
    private final SecurityAlertService alertService;
    private final AuditLedgerService auditLedgerService;

    public DescongelamentoContaService(SecurityDualApprovalService dualApprovalService,
                                       UserSecurityProfileRepository profileRepo,
                                       UsuarioRepository usuarioRepository,
                                       SecurityAlertService alertService,
                                       AuditLedgerService auditLedgerService) {
        this.dualApprovalService = Objects.requireNonNull(dualApprovalService);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.alertService = Objects.requireNonNull(alertService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public SecurityDualApprovalRequest solicitar(Usuario administrador, Long dispositivoId, Long usuarioCongeladoId, String caminho) {
        exigirAdministrador(administrador);
        if (Objects.equals(administrador.getId(), usuarioCongeladoId)) {
            throw new AccessDeniedPjbException("descongelamento_da_propria_conta");
        }
        UserSecurityProfile perfil = perfilCongelado(usuarioCongeladoId);
        SecurityDualApprovalRequest pedido = dualApprovalService.request(administrador, dispositivoId, null, ACAO, "POST", caminho,
                "panic-unfreeze", hashDaAcao(usuarioCongeladoId, perfil.getFrozenAt()), VALIDADE_DA_APROVACAO_SEGUNDOS);
        auditLedgerService.appendSafely("ACCOUNT_UNFREEZE_REQUESTED", "SECURITY", String.valueOf(usuarioCongeladoId),
                "solicitante=" + administrador.getId() + " aprovacao=" + pedido.getId());
        return pedido;
    }

    @Transactional
    public void executar(Usuario administrador, Long aprovacaoId, Long usuarioCongeladoId) {
        exigirAdministrador(administrador);
        UserSecurityProfile perfil = perfilCongelado(usuarioCongeladoId);
        if (!dualApprovalService.isApprovedFor(administrador, aprovacaoId, hashDaAcao(usuarioCongeladoId, perfil.getFrozenAt()), null)) {
            throw new AccessDeniedPjbException("aprovacao_invalida");
        }
        Usuario aprovador = dualApprovalService.getRequired(aprovacaoId).getApprovedBy();
        exigirAdministrador(aprovador);
        if (profileRepo.findByUsuarioId(aprovador.getId()).filter(UserSecurityProfile::isFrozenNow).isPresent()) {
            throw new AccessDeniedPjbException("aprovador_congelado");
        }
        if (!dualApprovalService.consumirAprovacao(aprovacaoId)) {
            throw new AccessDeniedPjbException("aprovacao_ja_utilizada");
        }

        perfil.setFrozenAt(null);
        perfil.setFrozenUntil(null);
        perfil.setFreezeReason(null);
        profileRepo.save(perfil);

        Usuario titular = usuarioRepository.findById(usuarioCongeladoId)
                .orElseThrow(() -> new IllegalArgumentException("usuario_nao_encontrado"));
        alertService.create(titular, "ACCOUNT_UNFROZEN", "Conta descongelada",
                "Conta liberada pela administracao com dupla aprovacao.", null, 60);
        auditLedgerService.appendSafely("ACCOUNT_UNFROZEN", "SECURITY", String.valueOf(usuarioCongeladoId),
                "solicitante=" + administrador.getId() + " aprovador=" + aprovador.getId() + " aprovacao=" + aprovacaoId);
    }

    private UserSecurityProfile perfilCongelado(Long usuarioId) {
        return profileRepo.findByUsuarioId(usuarioId)
                .filter(UserSecurityProfile::isFrozenNow)
                .orElseThrow(() -> new IllegalArgumentException("conta_nao_congelada"));
    }

    private static void exigirAdministrador(Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null || !usuario.getTipoUsuario().isAdmin()) {
            throw new AccessDeniedPjbException("papel_administrador_obrigatorio");
        }
    }

    static String hashDaAcao(Long usuarioCongeladoId, LocalDateTime congeladoEm) {
        return Hashes.sha256Hex(ACAO + "|" + usuarioCongeladoId + "|" + congeladoEm);
    }
}
